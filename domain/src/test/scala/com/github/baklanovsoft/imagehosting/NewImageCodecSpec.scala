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
            |    "bucketId": "95fb5f81-9280-4a8f-850f-2f3438bcfe24",
            |    "imageId": "22c403e8-3093-485a-834f-542504601e88"
            |}
            |""".stripMargin
        )
        .value

    val expected =
      NewImage(
        BucketId(UUID.fromString("95fb5f81-9280-4a8f-850f-2f3438bcfe24")),
        ImageId(UUID.fromString("22c403e8-3093-485a-834f-542504601e88"))
      )

    json.as[NewImage].value mustBe expected: Unit

    expected.asJson mustBe json
  }

}
