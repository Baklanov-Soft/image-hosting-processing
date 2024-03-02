package com.github.baklanovsoft.imagehosting

import com.github.baklanovsoft.imagehosting.common.NewtypeCodecs
import io.circe.Codec
import io.circe.generic.AutoDerivation
import io.circe.generic.semiauto.deriveCodec

final case class Categories(
    bucketId: BucketId,
    imageId: ImageId,
    categories: Map[Category, Score]
)

object Categories extends NewtypeCodecs with AutoDerivation {
  implicit val codec: Codec[Categories] = deriveCodec
}
