package com.github.baklanovsoft

import io.estatico.newtype.macros.newtype

import java.util.UUID

package object imagehosting {
  @newtype final case class BucketId(value: UUID)

  @newtype final case class Prefix(value: UUID)

  @newtype final case class ImageName(value: String)

  @newtype final case class Category(value: String)

  @newtype final case class Score(value: Double)

}
