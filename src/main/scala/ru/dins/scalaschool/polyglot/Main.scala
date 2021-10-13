package ru.dins.scalaschool.polyglot

import ru.dins.scalaschool.polyglot.configuration.Config
import zio._
import zio.console.putStrLn
import zio.magic._
import canoe.api._
import canoe.models.CallbackButtonSelected
import canoe.models.Update
import canoe.models.outgoing.TextContent
import zio.interop.catz._
import zio.interop.catz.implicits._
import ru.dins.scalaschool.polyglot.db.Database
import ru.dins.scalaschool.polyglot.client.HttpClient
import ru.dins.scalaschool.polyglot.repository._
import ru.dins.scalaschool.polyglot.scenario._
import ru.dins.scalaschool.polyglot.client.HttpClient.HttpClient
import fs2._
import ru.dins.scalaschool.polyglot.jobs.RepetitionJob

object Main extends CatsApp {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] = {
    val program = for {
      config                 <- Config.get
      _                      <- FlywayMigration.migrate(config)
      dictionaryRepository   <- DictionaryRepository.make()
      wordRepository         <- WordRepository.make()
      subscriptionRepository <- SubscriptionRepository.make()
      client                 <- startUpBot(PolyglotScenario.makeScenarios(wordRepository, dictionaryRepository, subscriptionRepository))
      _ <- RepetitionJob(wordRepository, dictionaryRepository, subscriptionRepository)
        .sendRepetitionMessages(client)
        .forkDaemon
      _ <- ZIO.never
    } yield ()

    program
      .tapError(e => putStrLn(s"Execution failed with: ${e.getMessage}"))
      .injectCustom(Config.live, HttpClient.live, Database.live)
      .exitCode
  }

  private def startUpBot(scenarios: List[PolyglotScenario]): RIO[HttpClient with Has[Config], TelegramClient[Task]] = {
    def initBot(scenarios: List[PolyglotScenario])(implicit tgClient: TelegramClient[Task]): Task[Unit] =
      Bot
        .polling[Task]
        .follow(scenarios.map(_.scenario): _*)
        .through(answerCallbacks)
        .compile
        .drain

    for {
      config <- Config.get
      client <- createTelegramClient(config.token)
      _      <- initBot(scenarios)(client).forkDaemon
    } yield client
  }

  private def createTelegramClient(token: String): URIO[HttpClient, TelegramClient[Task]] =
    for {
      client <- HttpClient.client
    } yield TelegramClient.fromHttp4sClient(token)(client)

  def answerCallbacks(implicit tc: TelegramClient[Task]): Pipe[Task, Update, Update] =
    _.evalTap {
      case CallbackButtonSelected(_, query) =>
        query.data match {
          case Some(cbd) =>
            for {
              _ <- ZIO.foreach(query.message)(_.chat.send(content = TextContent(cbd)))
              _ <- query.finish
            } yield ()
          case _ => Task.unit
        }
      case _ => Task.unit
    }
}
