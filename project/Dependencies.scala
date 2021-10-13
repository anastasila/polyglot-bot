import sbt._

object Dependencies {
  object Versions {
    val scalatest      = "3.2.2"
    val cats           = "2.0.0"
    val zio            = "1.0.7"
    val zioCats        = "2.4.1.0"
    val zioMagic       = "0.3.2"
    val circe          = "0.12.3"
    val doobie         = "0.12.1"
    val fs2            = "2.1.0"
    val pureconfig     = "0.15.0"
    val flyway         = "7.8.2"
    val testContainers = "0.39.3"
    val logback        = "1.2.3"
    val canoe          = "0.5.1"
    val slf4zio        = "1.0.0"
  }

  lazy val scalaTest = Seq(
    "org.scalatest" %% "scalatest"                       % Versions.scalatest,
    "com.dimafeng"  %% "testcontainers-scala-postgresql" % Versions.testContainers,
  )

  lazy val cats = Seq(
    "org.typelevel" %% "cats-effect",
    "org.typelevel" %% "cats-core",
  ) map (_ % Versions.cats)

  lazy val zio = Seq(
    "dev.zio" %% "zio",
    "dev.zio" %% "zio-test",
    "dev.zio" %% "zio-test-sbt",
  ) map (_ % Versions.zio)

  lazy val zioCats = Seq(
    "dev.zio" %% "zio-interop-cats",
  ) map (_ % Versions.zioCats)

  lazy val zioMagic = Seq(
    "io.github.kitlangton" %% "zio-magic",
  ) map (_ % Versions.zioMagic)

  lazy val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser",
    "io.circe" %% "circe-literal",
  ).map(_ % Versions.circe)

  lazy val doobie = Seq(
    "org.tpolecat" %% "doobie-core",
    "org.tpolecat" %% "doobie-postgres",
    "org.tpolecat" %% "doobie-h2",
    "org.tpolecat" %% "doobie-hikari",
  ).map(_ % Versions.doobie)

  lazy val fs2 = Seq(
    "co.fs2" %% "fs2-core",
    "co.fs2" %% "fs2-io",
  ).map(_ % Versions.fs2)

  lazy val pureconfig = Seq(
    "com.github.pureconfig" %% "pureconfig",
  ).map(_ % Versions.pureconfig)

  lazy val flyway = Seq("org.flywaydb" % "flyway-core").map(_ % Versions.flyway)

  lazy val logging = Seq(
    "ch.qos.logback"     % "logback-classic" % Versions.logback,
    "com.github.mlangc" %% "slf4zio"         % Versions.slf4zio,
  )

  lazy val canoe = Seq(
    "org.augustjune" %% "canoe" % Versions.canoe,
  )
}
