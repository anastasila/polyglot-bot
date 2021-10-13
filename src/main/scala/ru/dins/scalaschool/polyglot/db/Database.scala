package ru.dins.scalaschool.polyglot.db

import cats.effect.Blocker
import doobie.ExecutionContexts
import doobie.hikari.HikariTransactor
import doobie.util.transactor.Transactor
import doobie.implicits._
import ru.dins.scalaschool.polyglot.configuration.Config
import zio.blocking.Blocking
import zio.{Has, RLayer, RManaged, Task, URIO, ZIO, ZLayer}
import zio.interop.catz._

object Database {
  type DBTransactor = Has[Transactor[Task]]

  val transactor: URIO[DBTransactor, Transactor[Task]] = ZIO.service[Transactor[Task]]

  val live: RLayer[Blocking with Has[Config], DBTransactor] =
    ZLayer.fromManaged(
      for {
        config <- Config.get.toManaged_
        xa     <- createTransactor(config)
      } yield xa,
    )

  private def createTransactor(config: Config): RManaged[Blocking, HikariTransactor[Task]] =
    ZIO.runtime[Blocking].toManaged_.flatMap { implicit rt =>
      val transactorResource = for {
        connectExecutor <- ExecutionContexts.fixedThreadPool[Task](16)
        blockingExecutor = rt.environment.get.blockingExecutor.asEC
        tx <- HikariTransactor.newHikariTransactor[Task](
          Config.DefaultDriver,
          config.url,
          config.user,
          config.pass,
          connectExecutor,
          Blocker.liftExecutionContext(blockingExecutor),
        )
      } yield tx

      transactorResource.toManagedZIO
    }
}
