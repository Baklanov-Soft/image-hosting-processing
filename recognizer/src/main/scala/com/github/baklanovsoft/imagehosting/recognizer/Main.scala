package com.github.baklanovsoft.imagehosting.recognizer

import cats.syntax.applicative._
import cats.effect._
import com.github.baklanovsoft.imagehosting.NewImage
import com.github.baklanovsoft.imagehosting.kafka.{KafkaConsumer, KafkaJsonDeserializer}
import com.github.baklanovsoft.imagehosting.s3.MinioClient
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

object Main extends IOApp with KafkaJsonDeserializer {

  private def app: IO[Unit] = for {
    implicit0(loggerFactory: LoggerFactory[IO]) <- IO(Slf4jFactory.create[IO])

    config <- ConfigSource.default.loadF[IO, AppConfig]()

    logger <- loggerFactory.create

    _ <- logger.info(s"Hello world! Config: $config")

    minioClient =
      MinioClient.of[IO](host = config.minio.host, username = config.minio.user, password = config.minio.password)

    imageConsumer = new KafkaConsumer[IO, Unit, NewImage](
                      config.kafkaBootstrapServers,
                      config.consumerGroupId,
                      config.newImagesTopic
                    )

    resources = for {
                  detection <- if (config.debugCategories) ObjectDetection.debug[IO](minioClient)
                               else ObjectDetection.production[IO]

                  nsfw <- {
                    if (config.enableNsfwDetection) NsfwDetection.of[IO](config.nsfwModelPath, config.nsfwSynsetPath)
                    else
                      Resource.eval {
                        logger.warn("NSFW Detection is disabled") *>
                          NsfwDetection.dummy[IO].pure[IO]
                      }
                  }

                  categorization <- Resource.eval(
                                      CategorizationStream
                                        .of[IO](
                                          imageConsumer,
                                          minioClient,
                                          detection,
                                          nsfw,
                                          config.kafkaBootstrapServers,
                                          config.categoriesTopic
                                        )(
                                          implicitly[Temporal[IO]]
                                        )
                                        .map(_.streamR)
                                    )
                  stream         <- categorization
                } yield stream

    _ <- resources.use(_.compile.drain)
  } yield ()

  override def run(args: List[String]): IO[ExitCode] =
    app.as(ExitCode.Success)
}
