import Dependencies._

ThisBuild / scalaVersion := "2.13.4"
ThisBuild / version := "0.1"
ThisBuild / organization := "ru.dins.scalaschool"

lazy val `Polyglot-bot` = (project in file("."))
  .settings(
    name := "Polyglot-bot",
    dockerExposedPorts += 8080,
    dockerBaseImage := "adoptopenjdk/openjdk11",
    dockerRepository := Some("eu.gcr.io/dins-scala-school"),
    version in Docker := s"${git.gitHeadCommit.value.map(_.take(7)).getOrElse("UNKNOWN")}",
    libraryDependencies ++= (logging ++ cats ++ zio ++ zioCats ++ zioMagic ++ canoe ++ circe ++ doobie ++ fs2 ++ pureconfig ++ flyway ++ scalaTest
      .map(_ % Test)),
  ).enablePlugins(DockerPlugin, JavaAppPackaging)
