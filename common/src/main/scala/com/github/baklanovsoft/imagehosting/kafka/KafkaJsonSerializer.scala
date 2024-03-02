package com.github.baklanovsoft.imagehosting.kafka

import cats.effect.kernel.Sync
import fs2.kafka._
import io.circe.Encoder
import io.circe.syntax._

import java.nio.charset.Charset

trait KafkaJsonSerializer {

  private val cs = Charset.forName("UTF-8")

  implicit def serializer[F[_]: Sync, T: Encoder]: Serializer[F, T] =
    Serializer.instance[F, T] { (_, _, element) =>
      Sync[F].delay(
        element.asJson.noSpaces.getBytes(cs)
      )
    }

}
