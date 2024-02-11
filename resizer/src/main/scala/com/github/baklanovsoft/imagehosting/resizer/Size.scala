package com.github.baklanovsoft.imagehosting.resizer

import enumeratum.{Enum, EnumEntry}

sealed abstract class Size(val size: Int, val folder: String) extends EnumEntry

/** Sizes to resize pictures with
  */
object Sizes extends Enum[Size] {
  case object Size250 extends Size(250, "250")
  case object Size500 extends Size(500, "500")

  override def values: IndexedSeq[Size] = findValues
}
