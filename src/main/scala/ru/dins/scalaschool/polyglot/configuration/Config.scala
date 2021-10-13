package ru.dins.scalaschool.polyglot.configuration

import zio.{Has, URIO, URLayer, ZIO}
import zio.system._

final case class Config(host: String, port: String, user: String, pass: String, dbName: String, token: String) {
  val url: String = s"jdbc:postgresql://$host:$port/$dbName"
}

object Config {

  val DefaultDriver = "org.postgresql.Driver"

  private val TestToken             = "1827785732:AAG0epoHkyLohjFLKtzJMTHqNL_CfSy-hfA"
  private val ProdToken             = "1762729729:AAFm1Jp-ZjNIu22bZYcM-pD1DWZkDAp4Qxk"
  private val DefaultConfig: Config = Config("localhost", "5432", "postgres", "postgres", "postgres", TestToken)

  val get: URIO[Has[Config], Config] = ZIO.service[Config]

  val live: URLayer[System, Has[Config]] =
    (for {
      host <- env("DB_HOST").some
      port <- env("DB_PORT").some
      user <- env("DB_USER").some
      pass <- env("DB_PASS").some
      name <- env("DB_NAME").some
    } yield Config(host, port, user, pass, name, ProdToken)).optional
      .someOrElse(Config.DefaultConfig)
      .toLayer
      .orDie
}
