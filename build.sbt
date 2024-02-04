import Dependencies._

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.github.baklanovsoft"

ThisBuild / scalacOptions += "-Ymacro-annotations"
ThisBuild / libraryDependencies ++= Dependencies.plugins

val assemblyStrategy = assembly / assemblyMergeStrategy := {
  // to not apply local development override configurations
  case PathList("params.conf")                                                      =>
    MergeStrategy.discard

  // openapi docs generation
  case PathList("META-INF", "maven", "org.webjars", "swagger-ui", "pom.properties") =>
    MergeStrategy.singleOrError

  // lot of metainf folders might override this project's metainf which will result in error:
  // Could not find or load main class com.github.baklanovsoft.imagehosting.resizer.Main

  case PathList("META-INF", xs @ _*) =>
    xs.map(_.toLowerCase) match {
      // allow some metainf such as logback be written
      case "services" :: xs =>
        MergeStrategy.filterDistinctLines
      case _                => MergeStrategy.discard
    }

  // deduplicate error because of logback, this will fix
  case x                             =>
    MergeStrategy.first
}

lazy val domain = (project in file("domain"))
  .settings(
    name := "image-hosting-processing-domain"
  )
  .settings(
    libraryDependencies ++= Seq(
      newtype,
      enumeratum
    ) ++ codecs
  )
  .settings(
    libraryDependencies += Testing.scalatest
  )

lazy val common = (project in file("common"))
  .settings(
    name := "image-hosting-processing-common"
  )
  .settings(
    libraryDependencies ++= Seq(
      cats,
      catsEffect,
      fs2Kafka,
      minioClient
    ) ++ Seq(fs2).flatten
  )
  .dependsOn(domain)

lazy val resizer = (project in file("resizer"))
  .settings(
    name := "image-hosting-processing-resizer"
  )
  .settings(
    assemblyStrategy,
    // for no main manifest attribute error
    assembly / mainClass := Some("com.github.baklanovsoft.imagehosting.resizer.Main")
  )
  .settings(
    libraryDependencies ++= Seq(
      pureconfig,
      logging
    ).flatten
  )
  .dependsOn(domain, common)
