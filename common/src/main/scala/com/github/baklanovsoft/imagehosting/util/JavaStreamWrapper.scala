package com.github.baklanovsoft.imagehosting.util

import cats.implicits._
import cats.effect.kernel.{Resource, Sync}

import java.io.{InputStream, OutputStream}

/** This project uses Java streams a lot so we need to wrap them to Scala resource to simplify closure
  */
object JavaStreamWrapper {

  type JavaStream[F[_]] = Resource[F, InputStream]

  def wrap[F[_]: Sync](inputStream: InputStream): JavaStream[F] =
    Resource.make(
      Sync[F].pure(inputStream)
    )(is => Sync[F].delay(is.close()))

  def wrapSelfWriting[F[_]: Sync](out: OutputStream, in: InputStream): JavaStream[F] =
    Resource
      .make(
        Sync[F].pure((out, in))
      ) {
        // close both stream after resource used
        case (out, in) =>
          Sync[F].delay(in.close()) >>
            Sync[F].delay(out.close())
      }
      .map(_._2)

}
