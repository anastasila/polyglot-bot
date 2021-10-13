package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.AskDictionaryName
import ru.dins.scalaschool.polyglot.models.Word
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio._

final class DisplayWordsScenario(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository)
    extends PolyglotScenario {

  import DisplayWordsScenario._

  override val chatCommand: PolyglotCommand = PolyglotCommand("words", "Get list of all words from the dictionary.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat  <- Scenario.expect(command(chatCommand.tag).chat)
      words <- provideWords(chat, dictionaryRepository, wordRepository)
      _     <- sendMessage(chat, displayWords(words))
    } yield ()
}

object DisplayWordsScenario {
  private def displayWords(words: List[Word]) =
    s"${words.size} words: \n" +
      words.map(s => s"— ${s.word}").mkString("\n")

  def apply(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository): DisplayWordsScenario =
    new DisplayWordsScenario(wordRepository, dictionaryRepository)
}
