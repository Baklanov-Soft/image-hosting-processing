package com.github.baklanovsoft.imagehosting

import com.github.baklanovsoft.imagehosting.common.NewtypeCodecs
import io.circe.Codec
import io.circe.generic.AutoDerivation
import io.circe.generic.semiauto.deriveCodec
import io.scalaland.chimney.Transformer
import io.scalaland.chimney.dsl._

/** Information for s3 on how to extract/put image
  */
final case class ImageMeta(
    bucket: BucketId,
    prefix: Prefix,
    name: ImageName
) {
  val path = s"$prefix/$name"

  override def toString: String =
    s"$bucket/$path"

  def rename(newName: ImageName): ImageMeta =
    this.copy(name = newName)
}

object ImageMeta extends NewtypeCodecs with AutoDerivation {

  implicit val codec: Codec[NewImage] = deriveCodec

  implicit val transformer: Transformer[NewImage, ImageMeta] =
    (src: NewImage) =>
      src
        .into[ImageMeta]
        .withFieldRenamed(_.image, _.name)
        .transform
}
