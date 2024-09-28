package com.github.baklanovsoft.imagehosting.s3

import cats.effect.kernel.{Resource, Sync}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.util.JavaStreamWrapper
import com.github.baklanovsoft.imagehosting.util.JavaStreamWrapper.JavaStream
import com.github.baklanovsoft.imagehosting.{BucketId, ImageMeta}
import io.minio.{GetObjectArgs, MakeBucketArgs, MinioClient => MinioClientJava, PutObjectArgs, RemoveBucketArgs}

import java.io.InputStream

trait MinioClient[F[_]] {
  def putImage(
      imageMeta: ImageMeta,
      stream: JavaStream[F],
      contentType: String
  ): F[Unit]

  def getImage(imageMeta: ImageMeta): JavaStream[F]

  // those are not really used since storage app manages the buckets
  def makeBucket(bucketId: BucketId): F[Unit]
  def dropBucket(bucketId: BucketId): F[Unit]
}

object MinioClient {

  def of[F[_]: Sync](host: String, username: String, password: String): MinioClient[F] =
    new MinioClient[F] {
      private val client =
        MinioClientJava
          .builder()
          .endpoint(host)
          .credentials(username, password)
          .build()

      override def putImage(
          imageMeta: ImageMeta,
          stream: JavaStream[F],
          contentType: String
      ): F[Unit] =
        stream.use { inputStream =>
          Sync[F].delay {
            client
              .putObject(
                PutObjectArgs
                  .builder()
                  .bucket(imageMeta.bucket.value.toString)
                  .`object`(imageMeta.path)
                  .stream(inputStream, -1, 1024 * 1024 * 5)
                  .contentType(contentType)
                  .build()
              )

          }.void
        }

      override def getImage(
          imageMeta: ImageMeta
      ): JavaStream[F] =
        Resource
          .eval(
            Sync[F]
              .delay {
                val inputStream: InputStream =
                  client
                    .getObject(
                      GetObjectArgs
                        .builder()
                        .bucket(imageMeta.bucket.value.toString)
                        .`object`(imageMeta.path)
                        .build()
                    )

                inputStream
              }
          )
          .flatMap(inputStream => JavaStreamWrapper.wrap(inputStream))

      override def makeBucket(bucketId: BucketId): F[Unit] =
        Sync[F].delay {
          client.makeBucket(MakeBucketArgs.builder().bucket(bucketId.value.toString).build())
        }

      override def dropBucket(bucketId: BucketId): F[Unit] =
        Sync[F].delay {
          client.removeBucket(RemoveBucketArgs.builder().bucket(bucketId.value.toString).build())
        }
    }

}
