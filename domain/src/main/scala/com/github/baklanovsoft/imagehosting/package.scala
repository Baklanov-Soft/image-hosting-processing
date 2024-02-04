package com.github.baklanovsoft

import io.estatico.newtype.macros.newtype

import java.util.UUID

package object imagehosting {
  @newtype final case class BucketId(value: UUID)

  @newtype final case class ImageId(value: UUID)
}
