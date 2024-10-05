package com.github.baklanovsoft.imagehosting

import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import io.circe.parser
import io.circe.syntax._

import java.util.UUID

class NewImageCodecSpec extends AnyFunSuite with Matchers with EitherValues {

  test("NewImage codecs should encode and decode") {
    val json =
      parser
        .parse(
          """
            |{
            |    "bucket": "00000000-0000-0000-0000-000000000000",
            |    "prefix": "557b036f-c61f-40b6-ba13-4708519a566f",
            |    "name": "original.jpg"
            |}
            |""".stripMargin
        )
        .value

    val expected =
      NewImage(
        BucketId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
        Prefix(UUID.fromString("557b036f-c61f-40b6-ba13-4708519a566f")),
        ImageName("original.jpg")
      )

    json.as[NewImage].value mustBe expected: Unit

    expected.asJson mustBe json
  }

}
