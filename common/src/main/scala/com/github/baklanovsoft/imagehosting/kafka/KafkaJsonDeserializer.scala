package com.github.baklanovsoft.imagehosting.kafka

import cats.effect.kernel.Sync
import cats.implicits._
import com.github.baklanovsoft.imagehosting.error.DecodingError
import KafkaJsonDeserializer.KafkaJsonDecodingError
import fs2.kafka._
import io.circe.Decoder
import io.circe.jawn.decodeByteArray

trait KafkaJsonDeserializer {

  /** Need to specify it explicitly so it won't try to deserialize Unit with circe
    */
  implicit def unitDeserealizer[F[_]: Sync]: Deserializer[F, Unit] = Deserializer.unit[F]

  implicit def deserializer[F[_]: Sync, T: Decoder]: Deserializer[F, T] =
    Deserializer.instance { (_, _, bytes) =>
      decodeByteArray[T](bytes) match {
        case Left(value)  =>
          KafkaJsonDecodingError(
            s"Can't decode json kafka message: ${value.getMessage}. Original message: ${bytes}"
          )
            .raiseError[F, T]
        case Right(value) => value.pure[F]
      }
    }

}

object KafkaJsonDeserializer {
  final case class KafkaJsonDecodingError(message: String) extends DecodingError {
    override def toString: String = s"KafkaJsonDecodingError: $message"
  }
}
