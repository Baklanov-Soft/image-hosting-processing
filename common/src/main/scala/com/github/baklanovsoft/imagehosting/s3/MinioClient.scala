package com.github.baklanovsoft.imagehosting.s3

import cats.effect.kernel.Sync
import io.minio.{MakeBucketArgs, MinioClient => MinioClientJava, PutObjectArgs, RemoveBucketArgs}
import cats.implicits._
import com.github.baklanovsoft.imagehosting.BucketId

import java.io.InputStream

trait MinioClient[F[_]] {
  def makeBucket(bucketId: BucketId): F[Unit]
  def dropBucket(bucketId: BucketId): F[Unit]

  def putObject(bucketId: BucketId, folder: String, objectName: String, stream: InputStream): F[Unit]
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

      override def makeBucket(bucketId: BucketId): F[Unit] =
        Sync[F].delay {
          client.makeBucket(MakeBucketArgs.builder().bucket(bucketId.value.toString).build())
        }

      override def putObject(bucketId: BucketId, folder: String, objectName: String, stream: InputStream): F[Unit] =
        Sync[F].delay {
          val path = s"$folder/$objectName"

          client
            .putObject(
              PutObjectArgs
                .builder()
                .bucket(bucketId.value.toString)
                .`object`(path)
                .stream(stream, -1, 1024 * 1024 * 5)
                .build()
            )

        }.void

      override def dropBucket(bucketId: BucketId): F[Unit] =
        Sync[F].delay {
          client.removeBucket(RemoveBucketArgs.builder().bucket(bucketId.value.toString).build())
        }
    }

}
