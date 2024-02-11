package com.github.baklanovsoft.imagehosting.s3

import cats.effect.kernel.Sync
import cats.implicits._
import com.github.baklanovsoft.imagehosting.BucketId
import io.minio.{GetObjectArgs, MakeBucketArgs, MinioClient => MinioClientJava, PutObjectArgs, RemoveBucketArgs}

import java.io.InputStream

trait MinioClient[F[_]] {
  def makeBucket(bucketId: BucketId): F[Unit]
  def dropBucket(bucketId: BucketId): F[Unit]

  def putObject(bucketId: BucketId, objectName: String, stream: InputStream, folder: Option[String] = None): F[Unit]
  def getObject(bucketId: BucketId, objectName: String, folder: Option[String] = None): F[InputStream]
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

      override def putObject(
          bucketId: BucketId,
          objectName: String,
          stream: InputStream,
          folder: Option[String] = None
      ): F[Unit] =
        Sync[F].delay {

          val path = folder.fold(objectName)(f => s"$f/$objectName")

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

      override def getObject(
          bucketId: BucketId,
          objectName: String,
          folder: Option[String] = None
      ): F[InputStream] =
        Sync[F].delay {
          val path = folder.fold(objectName)(f => s"$f/$objectName")

          val inputStream: InputStream =
            client
              .getObject(
                GetObjectArgs
                  .builder()
                  .bucket(bucketId.value.toString)
                  .`object`(path)
                  .build()
              )

          inputStream
        }

      override def dropBucket(bucketId: BucketId): F[Unit] =
        Sync[F].delay {
          client.removeBucket(RemoveBucketArgs.builder().bucket(bucketId.value.toString).build())
        }
    }

}
