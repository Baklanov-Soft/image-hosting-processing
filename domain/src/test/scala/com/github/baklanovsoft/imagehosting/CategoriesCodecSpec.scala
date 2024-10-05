package com.github.baklanovsoft.imagehosting

import io.circe.parser
import io.circe.syntax._
import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers

import java.util.UUID

class CategoriesCodecSpec extends AnyFunSuite with Matchers with EitherValues {

  test("Categories codecs should encode and decode") {
    val json =
      parser
        .parse(
          """
            |{
            |  "bucket": "00000000-0000-0000-0000-000000000000",
            |  "prefix": "557b036f-c61f-40b6-ba13-4708519a566f",
            |  "name": "original.jpg",
            |  "categories": {
            |    "person": 0.9329947,
            |    "dog": 0.83488,
            |    "nsfw": 0.9342
            |  }
            |}
            |""".stripMargin
        )
        .value

    val expected =
      Categories(
        BucketId(UUID.fromString("00000000-0000-0000-0000-000000000000")),
        Prefix(UUID.fromString("557b036f-c61f-40b6-ba13-4708519a566f")),
        ImageName("original.jpg"),
        Map(
          Category("person") -> Score(0.9329947),
          Category("dog")    -> Score(0.83488),
          Category("nsfw")   -> Score(0.9342)
        )
      )

    json.as[Categories].value mustBe expected: Unit

    expected.asJson mustBe json
  }

}
