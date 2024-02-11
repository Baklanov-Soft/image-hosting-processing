package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.kernel.Sync
import org.imgscalr.Scalr

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import javax.imageio.ImageIO

class ResizeJob[F[_]: Sync] {

  def resize(originalImage: InputStream): F[List[(Size, InputStream)]] =
    Sync[F].bracket {
      Sync[F].delay {

        val originalImageBuf = ImageIO.read(originalImage)

        Sizes.values.toList
          // do not make image bigger than it is already
          .filterNot(size => size.size >= originalImageBuf.getHeight || size.size >= originalImageBuf.getWidth)
          .map { size =>
            val out = new ByteArrayOutputStream()

            val resizedImageBuf = Scalr.resize(originalImageBuf, size.size)
            ImageIO.write(resizedImageBuf, "jpeg", out)

            val in: InputStream = new ByteArrayInputStream(out.toByteArray)

            size -> (out, in)
          }
      }

    }(listOfStreams => Sync[F].delay(listOfStreams.map(t => t._1 -> t._2._2))) { listOfStreams =>
      Sync[F].delay {
        listOfStreams.foreach { case (_, (out, in)) =>
          out.close()
          in.close()
        }
        originalImage.close()
      }
    }

}
