package com.github.baklanovsoft.helloworld

import org.scalatest.EitherValues
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.must.Matchers
import io.circe.{Json, parser}

class HelloWorldSuit extends AnyFunSuite with Matchers with EitherValues {

  test("hello world must be successful") {
    val str =
      """
        |{
        | "message": "hello world!",
        | "id": 0
        |}
        |""".stripMargin

    parser.parse(str).value mustBe Json.obj(
      "message" -> Json.fromString("hello world!"),
      "id"      -> Json.fromInt(0)
    )
  }

}
