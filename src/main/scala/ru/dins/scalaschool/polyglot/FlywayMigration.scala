package ru.dins.scalaschool.polyglot

import ru.dins.scalaschool.polyglot.configuration.Config
import org.flywaydb.core.Flyway
import zio.Task

object FlywayMigration {
  def migrate(config: Config): Task[Unit] =
    Task {
      Flyway
        .configure(this.getClass.getClassLoader)
        .dataSource(config.url, config.user, config.pass)
        .group(true)
        .locations("migrations")
        .connectRetries(Int.MaxValue)
        .load()
        .migrate()
    }.unit
}
