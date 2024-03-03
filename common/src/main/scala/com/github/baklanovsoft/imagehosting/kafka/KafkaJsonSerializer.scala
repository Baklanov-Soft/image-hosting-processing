package com.github.baklanovsoft.imagehosting.kafka

import cats.effect.kernel.Sync
import fs2.kafka._
import io.circe.Encoder
import io.circe.syntax._

import java.nio.charset.Charset

trait KafkaJsonSerializer {

  private val cs = Charset.forName("UTF-8")

  // need to specify it implicitly because circe's serialization of unit is {} and its a valid key
  implicit def unitSerializer[F[_]: Sync]: Serializer[F, Unit] = Serializer.unit[F]

  implicit def serializer[F[_]: Sync, T: Encoder]: Serializer[F, T] =
    Serializer.instance[F, T] { (_, _, element) =>
      Sync[F].delay(
        element.asJson.noSpaces.getBytes(cs)
      )
    }

}
