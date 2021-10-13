package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.AskDictionaryName
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio._

final class RepeatWordScenario(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository)
    extends PolyglotScenario {

  override val chatCommand: PolyglotCommand = PolyglotCommand("repeatword", "Repeat random word from the dictionary.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat       <- Scenario.expect(command(chatCommand.tag).chat)
      dictionary <- provideNonEmptyDictionary(chat, dictionaryRepository, wordRepository)
      word       <- Scenario.eval(wordRepository.getRandomWord(dictionary.id))
      _          <- repeatWord(chat, word)
    } yield ()
}

object RepeatWordScenario {
  def apply(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository): RepeatWordScenario =
    new RepeatWordScenario(wordRepository, dictionaryRepository)
}
