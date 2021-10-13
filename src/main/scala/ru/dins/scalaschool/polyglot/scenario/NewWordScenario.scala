package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.models.Chat
import canoe.models.messages.TextMessage
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{
  AskDictionaryForNewWord,
  AskExample,
  AskWord,
  AskWordDefinition,
  ExistingWord,
  SuccessfulWordCreation,
}
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.models.{Dictionary, WordByNameNotFound, WordContainer}
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio._

final class NewWordScenario(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository)
    extends PolyglotScenario {

  override val chatCommand: PolyglotCommand = PolyglotCommand("addword", "Add a new word to the dictionary.")

  private val Skip = "skip"

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat       <- Scenario.expect(command(chatCommand.tag).chat)
      dictionary <- provideDictionaryByName(chat, dictionaryRepository)
      _          <- Scenario.eval(AskWord.sendTo(chat))
      word       <- provideWord(chat, dictionary)
      _          <- Scenario.eval(AskWordDefinition(word).sendTo(chat))
      definition <- checkTextLength(chat, TextLength)
      example    <- provideExample(chat)
      _          <- addWord(chat, dictionary, WordContainer(chat.id, dictionary.id, word, definition, example))
    } yield ()

  private def provideWord(chat: Chat, dictionary: Dictionary)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, String] =
    for {
      answer <- checkTextLength(chat, NameLength)
      word <- Scenario.eval(wordRepository.getWordByName(chat.id, dictionary.id, answer)).attempt.flatMap {
        case Right(w) =>
          Scenario.eval(ExistingWord(w.word).sendTo(chat)) >> provideWord(chat, dictionary)
        case Left(WordByNameNotFound(answer)) => Scenario.pure(answer)
        case _                                => defaultErrorScenario(chat) >> provideWord(chat, dictionary)
      }
    } yield word

  private def provideExample(chat: Chat)(implicit tc: TelegramClient[Task]): Scenario[Task, Option[String]] =
    for {
      _      <- Scenario.eval(AskExample.sendTo(chat, keyboard = createKeyboardWithSingleButton(Skip)))
      answer <- checkTextLength(chat, TextLength)
      example <- Scenario.pure(answer match {
        case skip if skip.equalsIgnoreCase(Skip) => None
        case other                               => Some(other)
      })
    } yield example

  private def addWord(chat: Chat, dictionary: Dictionary, container: WordContainer)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, TextMessage] =
    for {
      wordAdded <- Scenario.eval(wordRepository.addWord(container)).attempt
      response <- wordAdded match {
        case Right(word) =>
          Scenario.eval(SuccessfulWordCreation(word.word, dictionary.name).sendTo(chat))
        case _ => defaultErrorScenario(chat)
      }
    } yield response
}

object NewWordScenario {
  def apply(repository: WordRepository, dictionaryRepository: DictionaryRepository): NewWordScenario =
    new NewWordScenario(repository, dictionaryRepository)
}
