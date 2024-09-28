package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.Temporal
import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.{ImageMeta, ImageName, NewImage}
import com.github.baklanovsoft.imagehosting.kafka.KafkaConsumer
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import fs2.kafka.commitBatchWithin
import org.typelevel.log4cats.{Logger, LoggerFactory}
import io.scalaland.chimney.dsl._

import scala.concurrent.duration._

class ResizingStream[F[_]: Sync: Logger] private (
    imageConsumer: KafkaConsumer[F, Unit, NewImage],
    minioClient: MinioClient[F],
    resizer: ResizeJob[F]
)(temporal: Temporal[F]) {

  def streamR: Resource[F, fs2.Stream[F, Unit]] =
    imageConsumer.streamR
      .map(
        _.evalMap { commitable =>
          val msg = commitable.record.value
          val o   = commitable.offset.offsetAndMetadata.offset()
          val p   = commitable.offset.topicPartition.partition()

          {
            for {
              _ <- Logger[F].info(s"Kafka read [$p:$o] --- $msg")

              s3Image        = msg.transformInto[ImageMeta]
              originalImage <- minioClient.getImage(s3Image)

              _ <- resizer
                     .resize(originalImage)
                     .flatMap(listOfPreviews =>
                       listOfPreviews.traverse { case (size, stream) =>
                         val resizedImage = s3Image.rename(ImageName(size.name))

                         minioClient.putImage(
                           resizedImage,
                           stream,
                           contentType = "image/jpeg"
                         )

                       } *> Logger[F].info(s"Resized image ${msg.image} with sizes ${listOfPreviews.map(_._1)}")
                     )

            } yield ()
          }.as(commitable.offset)
        }
          .through(commitBatchWithin[F](100, 7.seconds)(temporal))
      )

}

object ResizingStream {

  def of[F[_]: Sync: LoggerFactory](
      imageConsumer: KafkaConsumer[F, Unit, NewImage],
      minioClient: MinioClient[F],
      resizer: ResizeJob[F]
  )(temporal: Temporal[F]): F[ResizingStream[F]] =
    LoggerFactory[F].create.map(implicit logger => new ResizingStream[F](imageConsumer, minioClient, resizer)(temporal))

}
