package com.github.baklanovsoft.imagehosting.recognizer

import ai.djl.modality.cv.ImageFactory
import cats.effect.Temporal
import cats.effect.kernel.{Async, Resource}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.{Categories, ImageMeta, NewImage}
import com.github.baklanovsoft.imagehosting.kafka.{KafkaConsumer, KafkaJsonSerializer}
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import fs2.Stream
import fs2.kafka._
import org.apache.kafka.common.TopicPartition
import org.typelevel.log4cats.{Logger, LoggerFactory}
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._

class CategorizationStream[F[_]: Async: Logger](
    imageConsumer: KafkaConsumer[F, Unit, NewImage],
    minioClient: MinioClient[F],
    detection: ObjectDetection[F],
    nsfw: NsfwDetection[F],
    kafkaBootstrap: String,
    categoriesTopic: String
)(temporal: Temporal[F])
    extends KafkaJsonSerializer {

  private val imageFactory = ImageFactory.getInstance()

  private def processRecord(imageMeta: ImageMeta): F[Categories] =
    minioClient
      .getImage(imageMeta)
      .use(imageStream =>
        for {
          image      <- Async[F].delay(imageFactory.fromInputStream(imageStream))
          categories <- detection.detect(image, imageMeta)
          nsfw0      <- nsfw.detect(image, imageMeta)
        } yield Categories(
          bucket = imageMeta.bucket,
          prefix = imageMeta.prefix,
          name = imageMeta.name,
          categories = categories ++ nsfw0
        )
      )

  /** Transactional fs2kafka stream
    */
  def streamR: Resource[F, Stream[F, ProducerResult[Unit, Categories]]] = {

    // transactional producer requires client id per-partition
    def producerSettings(partition: TopicPartition) =
      TransactionalProducerSettings(
        s"transactional-id-$partition",
        ProducerSettings[F, Unit, Categories]
          .withBootstrapServers(kafkaBootstrap)
          .withEnableIdempotence(true)
          .withRetries(10)
      )

    imageConsumer.streamPerPartitionR.map(insideResource =>
      insideResource
        .map { streamPerTopic =>
          streamPerTopic.map { case (consumerPartition, consumerStream) =>
            TransactionalKafkaProducer
              .stream(producerSettings(consumerPartition))
              .flatMap { producer =>
                consumerStream
                  .mapAsync(maxConcurrent = 10) { commitable =>
                    val msg = commitable.record.value
                    val o   = commitable.offset.offsetAndMetadata.offset()
                    val p   = commitable.offset.topicPartition.partition()

                    val consumerOffset = commitable.offset

                    for {
                      _ <- Logger[F].info(s"Kafka read [$p:$o] --- $msg")

                      s3Image     = msg.transformInto[ImageMeta]
                      categories <- processRecord(s3Image)
                      record      = ProducerRecord(categoriesTopic, (), categories)
                    } yield
                      if (categories.isEmpty) // don't send empty categories
                        CommittableProducerRecords(Vector.empty, consumerOffset)
                      else
                        CommittableProducerRecords.one(record, consumerOffset)
                  }
                  .groupWithin(500, 15.seconds)(temporal)
                  .evalMap(producer.produce)
              }

          }
        }
        .flatMap { partitionsMap =>
          Stream.emits(partitionsMap.toVector).parJoinUnbounded
        }
    )
  }

}

object CategorizationStream {

  def of[F[_]: Async: LoggerFactory](
      imageConsumer: KafkaConsumer[F, Unit, NewImage],
      minioClient: MinioClient[F],
      detection: ObjectDetection[F],
      nsfw: NsfwDetection[F],
      kafkaBootstrap: String,
      categoriesTopic: String
  )(temporal: Temporal[F]): F[CategorizationStream[F]] =
    LoggerFactory[F].create.map(implicit logger =>
      new CategorizationStream[F](imageConsumer, minioClient, detection, nsfw, kafkaBootstrap, categoriesTopic)(
        temporal
      )
    )

}
