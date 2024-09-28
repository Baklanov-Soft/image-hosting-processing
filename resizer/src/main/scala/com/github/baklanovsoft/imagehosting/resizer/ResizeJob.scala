package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.Resource
import cats.effect.kernel.Sync
import cats.implicits._
import com.github.baklanovsoft.imagehosting.util.JavaStreamWrapper
import com.github.baklanovsoft.imagehosting.util.JavaStreamWrapper.JavaStream
import org.imgscalr.Scalr

import java.awt.image.BufferedImage
import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import javax.imageio.ImageIO

class ResizeJob[F[_]: Sync] {

  def resize(originalImage: JavaStream[F]): F[List[(Size, JavaStream[F])]] =
    originalImage.use { inputStream =>
      for {
        originalImageBuf <- Sync[F].delay(ImageIO.read(inputStream))
        sizes             = Sizes.values.toList
                              // do not make image bigger than it is already
                              .filterNot(size => size.size >= originalImageBuf.getHeight || size.size >= originalImageBuf.getWidth)
                              .map { size =>
                                (
                                  size,
                                  for {
                                    resize <- Resource.eval(Sync[F].delay(Scalr.resize(originalImageBuf, size.size)))
                                    stream <- imageForWrite(resize)
                                  } yield stream
                                )

                              }

      } yield sizes
    }

  private def imageForWrite(image: BufferedImage): JavaStream[F] = for {
    // empty stream to write image into it
    out <- Resource.pure(new ByteArrayOutputStream())
    // write image to that stream
    _   <- Resource.eval(Sync[F].delay(ImageIO.write(image, "jpeg", out)))
    // write content of stream to input stream to insert into s3
    in  <- Resource.eval(Sync[F].delay(new ByteArrayInputStream(out.toByteArray)))

    wrapped <- JavaStreamWrapper.wrapSelfWriting[F](out, in)
  } yield wrapped

}
