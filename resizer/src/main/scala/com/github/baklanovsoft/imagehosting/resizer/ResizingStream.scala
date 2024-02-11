package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.Temporal
import cats.effect.kernel.Resource
import cats.implicits._
import com.github.baklanovsoft.imagehosting.NewImage
import com.github.baklanovsoft.imagehosting.kafka.KafkaConsumer
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import fs2.kafka.commitBatchWithin
import io.circe.syntax._
import org.typelevel.log4cats.{Logger, LoggerFactory}

import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import scala.concurrent.duration._

class ResizingStream[F[_]: Temporal: Logger] private (
    imageConsumer: KafkaConsumer[F, Unit, NewImage],
    minioClient: MinioClient[F]
) {

  def streamR: Resource[F, fs2.Stream[F, Unit]] =
    imageConsumer.streamR
      .map(
        _.evalMap { commitable =>
          val msg = commitable.record.value
          val o   = commitable.offset.offsetAndMetadata.offset()
          val p   = commitable.offset.topicPartition.partition()

          val streamingMessage = new ByteArrayInputStream(msg.asJson.noSpaces.getBytes(StandardCharsets.UTF_8))

          Logger[F].info(s"Kafka read [$p:$o] --- $msg") *>
            minioClient
              .putObject(
                msg.bucketId,
                "kafka-output",
                msg.imageId.value.toString,
                streamingMessage
              )
              .as(commitable.offset)
        }
          .through(commitBatchWithin[F](100, 7.seconds))
      )

}

object ResizingStream {

  def of[F[_]: Temporal: LoggerFactory](
      imageConsumer: KafkaConsumer[F, Unit, NewImage],
      minioClient: MinioClient[F]
  ): F[ResizingStream[F]] =
    LoggerFactory[F].create.map { implicit logger =>
      new ResizingStream[F](imageConsumer, minioClient)
    }

}
