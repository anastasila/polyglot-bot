package ru.dins.scalaschool.polyglot.repository

import ru.dins.scalaschool.polyglot.configuration.Config
import com.dimafeng.testcontainers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import zio.blocking.{Blocking, effectBlocking}
import zio._

object TestContainer {
  type Postgres = Has[PostgreSQLContainer]

  private val container: ZLayer[Blocking, Nothing, Postgres] =
    ZManaged.make {
      effectBlocking {
        val container = new PostgreSQLContainer(
          dockerImageNameOverride = Some("postgres:13.2").map(DockerImageName.parse),
        )
        container.start()
        container
      }.orDie
    }(container => effectBlocking(container.stop()).orDie).toLayer

  val postgresLayer: ZLayer[Blocking, Nothing, Postgres with Has[Config]] =
    container.map { postgres =>
      val container = postgres.get.container
      val config = Config(
        host = container.getHost,
        port = container.getMappedPort(5432).toString,
        user = container.getUsername,
        pass = container.getPassword,
        dbName = container.getDatabaseName,
        "",
      )

      postgres.++(Has(config))
    }
}
