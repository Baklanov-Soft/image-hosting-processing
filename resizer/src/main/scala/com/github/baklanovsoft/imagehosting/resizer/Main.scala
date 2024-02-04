package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.{ExitCode, IO, IOApp}
import com.github.baklanovsoft.imagehosting.NewImage
import com.github.baklanovsoft.imagehosting.imagehosting.kafka.{KafkaConsumer, KafkaJsonDeserializer}
import com.github.baklanovsoft.imagehosting.imagehosting.s3.MinioClient
import fs2.kafka.commitBatchWithin
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource
import io.circe.syntax._

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.UUID
import scala.concurrent.duration._

object Main extends IOApp with KafkaJsonDeserializer {

  private def app: IO[Unit] = for {
    implicit0(loggerFactory: LoggerFactory[IO]) <- IO(Slf4jFactory.create[IO])

    config <- ConfigSource.default.loadF[IO, AppConfig]()

    logger <- loggerFactory.create

    _ <- logger.info(s"Hello world! Config: $config")

    minio  = MinioClient.of[IO](host = config.minio.host, username = config.minio.user, password = config.minio.password)
    bucket = UUID.randomUUID().toString
    _     <- minio.makeBucket(bucket)
    _     <- logger.info(s"Created minio bucket: $bucket")

    _ <-
      new KafkaConsumer[IO, Unit, NewImage](
        config.kafkaBootstrapServers,
        config.consumerGroupId,
        config.newImagesTopic
      ).streamR
        .use(
          _.evalMap { commitable =>
            val msg = commitable.record.value
            val o   = commitable.offset.offsetAndMetadata.offset()
            val p   = commitable.offset.topicPartition.partition()

            val streamingMessage = new ByteArrayInputStream(msg.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))

            logger.info(s"Kafka read [$p:$o] --- $msg") *>
              minio
                .putObject(
                  bucket,
                  "kafka-output",
                  msg.imageId.value.toString,
                  streamingMessage
                )
                .as(commitable.offset)
          }
            .through(commitBatchWithin[IO](100, 7.seconds))
            .compile
            .drain
        )
  } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    app.as(ExitCode.Success)
}
