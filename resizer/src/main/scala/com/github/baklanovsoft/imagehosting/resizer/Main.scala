package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.{ExitCode, IO, IOApp}
import com.github.baklanovsoft.imagehosting.NewImage
import com.github.baklanovsoft.imagehosting.imagehosting.kafka.{KafkaConsumer, KafkaJsonDeserializer}
import fs2.kafka.commitBatchWithin
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax.CatsEffectConfigSource

import scala.concurrent.duration._

object Main extends IOApp with KafkaJsonDeserializer {

  private def app: IO[Unit] = for {
    implicit0(loggerFactory: LoggerFactory[IO]) <- IO(Slf4jFactory.create[IO])

    config <- ConfigSource.default.loadF[IO, AppConfig]()

    logger <- loggerFactory.create

    _ <- logger.info(s"Hello world! Config: $config")

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
            logger
              .info(s"Kafka read [$p:$o] --- $msg")
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
