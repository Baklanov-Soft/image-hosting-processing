import Dependencies._

ThisBuild / version      := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / organization := "com.github.baklanov-soft"

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
  .dependsOn(domain)

lazy val resizer = (project in file("resizer"))
  .settings(
    name := "image-hosting-processing-resizer"
  )
  .dependsOn(domain, common)
