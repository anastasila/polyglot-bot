package ru.dins.scalaschool.polyglot.scenario

import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, SubscriptionRepository}
import canoe.api._
import canoe.models.Chat
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{
  AskNameForNewDictionary,
  ExistingDictionary,
  SuccessfulDictionaryCreation,
}
import ru.dins.scalaschool.polyglot.models.{Dictionary, DictionaryAlreadyExists, SubscriptionNotFound}
import zio._

final class NewDictionaryScenario(repository: DictionaryRepository, subscriptionRepository: SubscriptionRepository)
    extends PolyglotScenario {
  override val chatCommand: PolyglotCommand = PolyglotCommand("newdictionary", "Create a new dictionary.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat     <- Scenario.expect(command(chatCommand.tag).chat)
      _        <- Scenario.eval(AskNameForNewDictionary.sendTo(chat))
      dictName <- checkTextLength(chat, NameLength)
      _ <- Scenario.eval(repository.createDictionary(chat.id, dictName)).attempt.flatMap {
        case Right(dictionary) => checkSubscription(chat, dictionary)
        case Left(DictionaryAlreadyExists(name)) =>
          Scenario.eval(ExistingDictionary(name).sendTo(chat))
        case _ => defaultErrorScenario(chat)
      }
    } yield ()

  private def checkSubscription(chat: Chat, dictionary: Dictionary)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, Unit] =
    for {
      _ <- Scenario.eval(subscriptionRepository.getSubscriptionByChatId(chat.id)).attempt.flatMap {
        case Right(_)                      => Scenario.eval(SuccessfulDictionaryCreation(dictionary.name).sendTo(chat))
        case Left(SubscriptionNotFound(_)) => addSubscription(chat, dictionary)
        case _                             => defaultErrorScenario(chat)
      }
    } yield ()

  private def addSubscription(chat: Chat, dictionary: Dictionary)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, Unit] =
    for {
      _ <- Scenario.eval(subscriptionRepository.addSubscription(chat.id))
      _ <- Scenario.eval(SuccessfulDictionaryCreation(dictionary.name).sendTo(chat))
    } yield ()
}

object NewDictionaryScenario {
  def apply(repository: DictionaryRepository, subscriptionRepository: SubscriptionRepository): NewDictionaryScenario =
    new NewDictionaryScenario(repository, subscriptionRepository)
}
