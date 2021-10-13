package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{AskDictionaryName, SuccessfulRepeat}
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio._

final class RepeatAllScenario(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository)
    extends PolyglotScenario {

  override val chatCommand: PolyglotCommand = PolyglotCommand("repeatall", "Repeat all words from the dictionary.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat       <- Scenario.expect(command(chatCommand.tag).chat)
      dictionary <- provideNonEmptyDictionary(chat, dictionaryRepository, wordRepository)
      words      <- Scenario.eval(wordRepository.getAllWordsByDictionary(chat.id, dictionary.id))
      _          <- words.map(repeatWord(chat, _)).reduceLeft(_ >> _)
      _          <- Scenario.eval(SuccessfulRepeat(dictionary.name).sendTo(chat))
    } yield ()
}

object RepeatAllScenario {
  def apply(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository): RepeatAllScenario =
    new RepeatAllScenario(wordRepository, dictionaryRepository)
}
