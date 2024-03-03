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
            |  "bucketId" : "95fb5f81-9280-4a8f-850f-2f3438bcfe24",
            |  "imageId" : "22c403e8-3093-485a-834f-542504601e88",
            |  "categories" : {
            |    "person" : 0.9329947,
            |    "dog" : 0.83488,
            |    "nsfw" : 0.9342
            |  }
            |}
            |""".stripMargin
        )
        .value

    val expected =
      Categories(
        BucketId(UUID.fromString("95fb5f81-9280-4a8f-850f-2f3438bcfe24")),
        ImageId(UUID.fromString("22c403e8-3093-485a-834f-542504601e88")),
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
