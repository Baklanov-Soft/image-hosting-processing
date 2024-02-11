package com.github.baklanovsoft.imagehosting.resizer

import cats.effect.kernel.Sync
import org.imgscalr.Scalr

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}
import javax.imageio.ImageIO

class ResizeJob[F[_]: Sync] {

  def resize(originalImage: InputStream): F[InputStream] =
    Sync[F].bracket {
      Sync[F].delay {

        val out = new ByteArrayOutputStream()

        val originalImageBuf = ImageIO.read(originalImage)
        val resizedImageBuf  = Scalr.resize(originalImageBuf, 100)
        ImageIO.write(resizedImageBuf, "jpeg", out)

        val in: InputStream = new ByteArrayInputStream(out.toByteArray)

        (out, in)
      }

    } { case (_, in) => Sync[F].delay(in) } { case (out, in) =>
      Sync[F].delay {
        in.close()
        out.close()
        originalImage.close()
      }
    }

}
