package ru.dins.scalaschool.polyglot.scenario
import canoe.api._
import canoe.models.Chat
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{
  AskInterval,
  MissingSubscription,
  SuccessfulIntervalChange,
  WrongInterval,
}
import ru.dins.scalaschool.polyglot.models.{Subscription, SubscriptionNotFound}
import zio._
import ru.dins.scalaschool.polyglot.repository.SubscriptionRepository
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand

final class ChangeIntervalScenario(subscriptionRepository: SubscriptionRepository) extends PolyglotScenario {
  override def chatCommand: PolyglotScenario.PolyglotCommand =
    PolyglotCommand("changeinterval", "Change interval for notifications (default: 6 hours).")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat         <- Scenario.expect(command(chatCommand.tag).chat)
      subscription <- getSubscription(chat)
      _ <- subscription match {
        case Some(sub) => changeInterval(chat, sub)
        case None      => Scenario.done[Task]
      }
    } yield ()

  private def getSubscription(chat: Chat)(implicit tc: TelegramClient[Task]): Scenario[Task, Option[Subscription]] =
    for {
      subscription <- Scenario.eval(subscriptionRepository.getSubscriptionByChatId(chat.id)).attempt.flatMap {
        case Right(sub) => Scenario.pure(Some(sub))
        case Left(SubscriptionNotFound(chatId)) =>
          for {
            _   <- Scenario.eval(MissingSubscription(chatId).sendTo(chat))
            sub <- Scenario.pure(None)
          } yield sub
        case Left(_) => defaultErrorScenario(chat) >> getSubscription(chat)
      }
    } yield subscription

  private def changeInterval(chat: Chat, subscription: Subscription)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, Unit] =
    for {
      _        <- Scenario.eval(AskInterval(subscription.interval).sendTo(chat))
      interval <- Scenario.expect(plainText)
      _ <- interval.toLongOption match {
        case Some(i) =>
          for {
            sub <- Scenario.eval(subscriptionRepository.changeInterval(chat.id, i))
            _   <- Scenario.eval(SuccessfulIntervalChange(sub.interval).sendTo(chat))
          } yield ()
        case None => Scenario.eval(WrongInterval.sendTo(chat)) >> changeInterval(chat, subscription)
      }
    } yield ()
}

object ChangeIntervalScenario {
  def apply(subscriptionRepository: SubscriptionRepository): ChangeIntervalScenario =
    new ChangeIntervalScenario(subscriptionRepository)
}
