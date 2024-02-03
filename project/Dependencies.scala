import sbt.*

object Dependencies {

  private object Versions {
    val apispec = "0.7.4"

    val cats       = "2.10.0"
    val catsEffect = "3.5.3"
    val circe      = "0.14.6"

    val enumeratum = "1.7.3"

    val fs2      = "3.9.4"
    val fs2Kafka = "3.2.0"

    val http4s = "0.23.25"

    val imgscalr = "4.2-release"

    val logback  = "1.4.14"
    val log4cats = "2.6.0"

    val newtype = "0.4.4"

    val pureconfig = "0.17.5"

    val tapir = "1.9.8"

    /* testing */

    val scalatest = "3.2.17"
    val weaver    = "0.8.4"

  }

  val plugins = Seq(
    ("org.typelevel" %% "kind-projector"     % "0.13.2").cross(CrossVersion.full),
    "com.olegpy"     %% "better-monadic-for" % "0.3.1"
  ).map(compilerPlugin)

  val apispec = "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml" % Versions.apispec

  val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  val codecs = Seq(
    "io.circe"                    %% "circe-core"       % Versions.circe,
    "io.circe"                    %% "circe-generic"    % Versions.circe,
    "io.circe"                    %% "circe-parser"     % Versions.circe,
    "com.beachape"                %% "enumeratum-circe" % Versions.enumeratum,
    "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-enumeratum" % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-newtype"    % Versions.tapir
  )

  val enumeratum = "com.beachape" %% "enumeratum" % Versions.enumeratum

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Versions.fs2,
    "co.fs2" %% "fs2-io"   % Versions.fs2
  )

  val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % Versions.fs2Kafka

  val http4s = Seq(
    "org.http4s" %% "http4s-core"         % Versions.http4s,
    "org.http4s" %% "http4s-server"       % Versions.http4s,
    "org.http4s" %% "http4s-ember-server" % Versions.http4s
  )

  val imagscalr = "com.github.rkalla" % "imgscalr" % Versions.imgscalr

  val logging = Seq(
    "ch.qos.logback" % "logback-core"  % Versions.logback,
    "org.typelevel" %% "log4cats-core" % Versions.log4cats
  )

  val newtype = "io.estatico" %% "newtype" % Versions.newtype

  val pureconfig = Seq(
    "com.github.pureconfig" %% "pureconfig"             % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-core"        % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-generic"     % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % Versions.pureconfig
  )

  val tapir = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-openapi-docs"  % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui"    % Versions.tapir,
    "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % Versions.tapir
  )

  object Testing {
    val scalatest = "org.scalatest" %% "scalatest" % Versions.scalatest % Test

    val weaver = Seq(
      "com.disneystreaming" %% "weaver-core" % Versions.weaver % Test,
      "com.disneystreaming" %% "weaver-cats" % Versions.weaver % Test
    )
  }

}
