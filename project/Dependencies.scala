import sbt.*

object Dependencies {

  private object Versions {
    val cats       = "2.12.0"
    val catsEffect = "3.5.4"
    val circe      = "0.14.10"

    val djl = "0.30.0"

    val enumeratum = "1.7.4"

    val fs2      = "3.11.0"
    val fs2Kafka = "3.5.1"

    val logback  = "1.5.8"
    val log4cats = "2.7.0"

    val minioClient = "8.5.12"

    val newtype = "0.4.4"

    val pureconfig = "0.17.7"

    val imgscalr = "4.2"

    /* testing */

    val scalatest = "3.2.19"
  }

  val plugins = Seq(
    ("org.typelevel" %% "kind-projector"     % "0.13.2").cross(CrossVersion.full),
    "com.olegpy"     %% "better-monadic-for" % "0.3.1"
  ).map(compilerPlugin)

  val cats       = "org.typelevel" %% "cats-core"   % Versions.cats
  val catsEffect = "org.typelevel" %% "cats-effect" % Versions.catsEffect

  val codecs = Seq(
    "io.circe"     %% "circe-core"       % Versions.circe,
    "io.circe"     %% "circe-generic"    % Versions.circe,
    "io.circe"     %% "circe-parser"     % Versions.circe,
    "com.beachape" %% "enumeratum-circe" % Versions.enumeratum
  )

  val djl = Seq(
    "ai.djl"         % "api"               % Versions.djl,
    // mxnet is used in object detection for embedded vgg16
    "ai.djl.mxnet"   % "mxnet-model-zoo"   % Versions.djl,
    "ai.djl.mxnet"   % "mxnet-engine"      % Versions.djl,
    // pytorch for nsfw detection
    "ai.djl.pytorch" % "pytorch-engine"    % Versions.djl,
    "ai.djl.pytorch" % "pytorch-model-zoo" % Versions.djl
  )

  val enumeratum = "com.beachape" %% "enumeratum" % Versions.enumeratum

  val fs2 = Seq(
    "co.fs2" %% "fs2-core" % Versions.fs2,
    "co.fs2" %% "fs2-io"   % Versions.fs2
  )

  val fs2Kafka = "com.github.fd4s" %% "fs2-kafka" % Versions.fs2Kafka

  val logging = Seq(
    "ch.qos.logback" % "logback-classic" % Versions.logback,
    "org.typelevel" %% "log4cats-core"   % Versions.log4cats,
    "org.typelevel" %% "log4cats-slf4j"  % Versions.log4cats
  )

  val minioClient = "io.minio" % "minio" % Versions.minioClient

  val newtype = "io.estatico" %% "newtype" % Versions.newtype

  val pureconfig = Seq(
    "com.github.pureconfig" %% "pureconfig"             % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-core"        % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-generic"     % Versions.pureconfig,
    "com.github.pureconfig" %% "pureconfig-cats-effect" % Versions.pureconfig
  )

  val imgscalr = "org.imgscalr" % "imgscalr-lib" % Versions.imgscalr

  object Testing {
    val scalatest = "org.scalatest" %% "scalatest" % Versions.scalatest % Test
  }

}
