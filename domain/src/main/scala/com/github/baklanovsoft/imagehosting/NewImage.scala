package com.github.baklanovsoft.imagehosting

import com.github.baklanovsoft.imagehosting.common.NewtypeCodecs
import io.circe.Codec
import io.circe.generic.AutoDerivation
import io.circe.generic.semiauto.deriveCodec

final case class NewImage(
    bucketId: BucketId,
    imageId: ImageId
)

object NewImage extends NewtypeCodecs with AutoDerivation {
  implicit val codec: Codec[NewImage] = deriveCodec
}
