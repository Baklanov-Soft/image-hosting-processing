package com.github.baklanovsoft.imagehosting.recognizer

import cats.implicits._
import cats.effect.kernel.{Async, Resource}
import cats.effect.{ExitCode, IO, IOApp}
import com.github.baklanovsoft.imagehosting.kafka.{KafkaConsumer, KafkaJsonDeserializer, KafkaJsonSerializer}
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import com.github.baklanovsoft.imagehosting.{Categories, Category, NewImage, Score}
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.common.TopicPartition
import org.typelevel.log4cats.{Logger, LoggerFactory}
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

import scala.concurrent.duration._

object Main extends IOApp with KafkaJsonDeserializer with KafkaJsonSerializer {
  private def app: IO[Unit] = for {
    implicit0(loggerFactory: LoggerFactory[IO]) <- IO(Slf4jFactory.create[IO])

    config <- ConfigSource.default.loadF[IO, AppConfig]()

    logger <- loggerFactory.create

    _ <- logger.info(s"Hello world! Config: $config")

    _ = MinioClient.of[IO](host = config.minio.host, username = config.minio.user, password = config.minio.password)

    imageConsumer = new KafkaConsumer[IO, Unit, NewImage](
                      config.kafkaBootstrapServers,
                      config.consumerGroupId,
                      config.newImagesTopic
                    )

    _ <-
      transactionalProcessing(imageConsumer, config.kafkaBootstrapServers, config.categoriesTopic).use(_.compile.drain)
  } yield ()

  private def transactionalProcessing[F[_]: Async: LoggerFactory](
      c: KafkaConsumer[F, Unit, NewImage],
      kafkaBootstrap: String,
      categoriesTopic: String
  ): Resource[F, Stream[F, ProducerResult[Unit, Categories]]] = {

    implicit val l = LoggerFactory.getLogger[F]

    // transactional producer requires client id per-partition
    def producerSettings(partition: TopicPartition) =
      TransactionalProducerSettings(
        s"transactional-id-$partition",
        ProducerSettings[F, Unit, Categories]
          .withBootstrapServers(kafkaBootstrap)
          .withEnableIdempotence(true)
          .withRetries(10)
      )

    c.streamPerPartitionR.map(insideResource =>
      insideResource
        .map { streamPerTopic =>
          streamPerTopic.map { case (partition, stream) =>
            TransactionalKafkaProducer
              .stream(producerSettings(partition))
              .flatMap { producer =>
                stream
                  .evalMap { commitable =>
                    val msg = commitable.record.value
                    val o   = commitable.offset.offsetAndMetadata.offset()
                    val p   = commitable.offset.topicPartition.partition()

                    val offset = commitable.offset

                    val c =
                      ProducerRecord(
                        categoriesTopic,
                        (),
                        Categories(
                          msg.bucketId,
                          msg.imageId,
                          Map(
                            Category("dog") -> Score(0.999999)
                          )
                        )
                      )

                    Logger[F].info(s"Kafka read [$p:$o] --- $msg") *>
                      Async[F].pure(CommittableProducerRecords.one(c, offset))
                  }
                  .groupWithin(500, 15.seconds)
                  .evalMap(producer.produce)
              }

          }
        }
        .flatMap { partitionsMap =>
          Stream.emits(partitionsMap.toVector).parJoinUnbounded
        }
    )
  }

  override def run(args: List[String]): IO[ExitCode] =
    app.as(ExitCode.Success)
}
