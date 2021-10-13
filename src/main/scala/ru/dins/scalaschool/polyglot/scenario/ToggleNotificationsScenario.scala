package ru.dins.scalaschool.polyglot.scenario
import canoe.api._
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{MissingSubscription, ToggleSubscription}
import ru.dins.scalaschool.polyglot.models.SubscriptionNotFound
import ru.dins.scalaschool.polyglot.repository.SubscriptionRepository
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import zio._

final class ToggleNotificationsScenario(subscriptionRepository: SubscriptionRepository) extends PolyglotScenario {
  override def chatCommand: PolyglotCommand =
    PolyglotCommand("togglenotifications", "Turn on or turn off notifications.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command(chatCommand.tag).chat)
      _ <- Scenario.eval(subscriptionRepository.toggleNotifications(chat.id)).attempt.flatMap {
        case Right(sub)                         => Scenario.eval(ToggleSubscription(sub.isActive).sendTo(chat))
        case Left(SubscriptionNotFound(chatId)) => Scenario.eval(MissingSubscription(chatId).sendTo(chat))
        case Left(_)                            => defaultErrorScenario(chat)
      }
    } yield ()
}

object ToggleNotificationsScenario {
  def apply(subscriptionRepository: SubscriptionRepository): ToggleNotificationsScenario =
    new ToggleNotificationsScenario(subscriptionRepository)
}
