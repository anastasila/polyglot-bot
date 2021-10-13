package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, SubscriptionRepository, WordRepository}
import zio.Task

trait PolyglotScenario {
  import PolyglotScenario._

  def chatCommand: PolyglotCommand
  def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit]
}

object PolyglotScenario {
  def makeScenarios(
      wordRepo: WordRepository,
      dictRepo: DictionaryRepository,
      subscriptionRepo: SubscriptionRepository,
  ): List[PolyglotScenario] = {
    val scenarios = List(
      NewDictionaryScenario(dictRepo, subscriptionRepo),
      NewWordScenario(wordRepo, dictRepo),
      RepeatWordScenario(wordRepo, dictRepo),
      RepeatAllScenario(wordRepo, dictRepo),
      DisplayDictionariesScenario(dictRepo),
      DisplayWordsScenario(wordRepo, dictRepo),
      DisplayOneWord(wordRepo, dictRepo),
      ToggleNotificationsScenario(subscriptionRepo),
      ChangeIntervalScenario(subscriptionRepo),
    )

    HelpScenario(scenarios.map(_.chatCommand)) :: scenarios
  }

  final case class PolyglotCommand(tag: String, description: String)
}
