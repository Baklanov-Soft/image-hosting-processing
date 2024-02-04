package com.github.baklanovsoft.imagehosting.imagehosting.kafka

import cats.effect.kernel.Sync
import cats.implicits._
import com.github.baklanovsoft.imagehosting.error.DecodingError
import com.github.baklanovsoft.imagehosting.imagehosting.kafka.KafkaJsonDeserializer.KafkaJsonDecodingError
import fs2.kafka._
import io.circe.Decoder
import io.circe.jawn.decodeByteArray

trait KafkaJsonDeserializer {

  implicit def deserializer[F[_]: Sync, T: Decoder]: Deserializer[F, T] =
    Deserializer.instance { (_, _, bytes) =>
      decodeByteArray[T](bytes) match {
        case Left(value)  =>
          KafkaJsonDecodingError(
            s"Can't decode json kafka message: ${value.getMessage}. Original message: ${new String(bytes)}"
          )
            .raiseError[F, T]
        case Right(value) => value.pure[F]
      }
    }

}

object KafkaJsonDeserializer {
  final case class KafkaJsonDecodingError(message: String) extends DecodingError
}
