package com.github

import io.estatico.newtype.macros.newtype

import java.util.UUID

package object baklanovsoft {
  @newtype final case class BucketId(value: UUID)

  @newtype final case class ImageId(value: UUID)
}
