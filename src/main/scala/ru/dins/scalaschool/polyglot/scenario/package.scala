package ru.dins.scalaschool.polyglot

import canoe.api._
import canoe.api.models.Keyboard
import canoe.models.ParseMode.Markdown
import canoe.syntax._
import canoe.models._
import canoe.models.messages.TextMessage
import canoe.models.outgoing.TextContent
import ru.dins.scalaschool.polyglot.models.ChatMessage.{
  AskAnswer,
  EmptyDictionary,
  GeneralMessage,
  IncorrectAnswer,
  LongText,
  MissingDictionary,
  MissingWord,
  UnexpectedError,
}
import ru.dins.scalaschool.polyglot.models.{Dictionary, DictionaryNotFound, Word, WordByNameNotFound, WordNotFound}
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio.Task

import scala.util.Random

package object scenario {

  val plainText: Expect[String] = text.andThen {
    case s if !s.startsWith("/") => s
  }

  val Congrats = List(
    "Well done! \uD83D\uDC4D",
    "Absolutely correct! \uD83D\uDC4C",
    "Right! \uD83D\uDC4D",
    "Good job! \uD83E\uDD29",
    "Keep it up! \uD83D\uDCAA",
    "Correct! \uD83D\uDC4D",
    "Nice! \uD83D\uDE00",
    "Wonderful! \uD83E\uDD29",
  )

  val ShowResult = "Show result"

  val NameLength       = 280
  val TextLength       = 500
  val MaxMessageLength = 4096

  private val MoreOption      = "More >>"
  private val StartOverOption = "<< From Beginning"
  private val MoreBtn         = KeyboardButton.text(MoreOption)
  private val StartOverBtn    = KeyboardButton.text(StartOverOption)

  def defaultErrorScenario(chat: Chat)(implicit tc: TelegramClient[Task]): Scenario[Task, TextMessage] =
    Scenario.eval(UnexpectedError.sendTo(chat))

  def provideDictionaryByName(chat: Chat, dictionaryRepository: DictionaryRepository)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, Dictionary] =
    for {
      dictName <- fetchDictionaryName(chat, dictionaryRepository)
      dictionary <- Scenario.eval(dictionaryRepository.getDictionaryByName(dictName)).attempt.flatMap {
        case Right(dictionary) => Scenario.pure(dictionary)
        case Left(DictionaryNotFound(dictName)) =>
          Scenario.eval(MissingDictionary(dictName).sendTo(chat)) >> provideDictionaryByName(chat, dictionaryRepository)
        case _ => defaultErrorScenario(chat) >> provideDictionaryByName(chat, dictionaryRepository)
      }
    } yield dictionary

  def provideNonEmptyDictionary(chat: Chat, dictionaryRepository: DictionaryRepository, wordRepository: WordRepository)(
      implicit tc: TelegramClient[Task],
  ): Scenario[Task, Dictionary] =
    for {
      dictionary <- provideDictionaryByName(chat, dictionaryRepository)
      words      <- Scenario.eval(wordRepository.getAllWordsByDictionary(chat.id, dictionary.id))
      result <-
        if (words.nonEmpty) Scenario.pure[Task](dictionary)
        else
          Scenario.eval(
            EmptyDictionary(dictionary.name).sendTo(chat),
          ) >> provideNonEmptyDictionary(chat, dictionaryRepository, wordRepository)
    } yield result

  def provideWords(chat: Chat, dictionaryRepository: DictionaryRepository, wordRepository: WordRepository)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, List[Word]] =
    for {
      dictionary <- provideDictionaryByName(chat, dictionaryRepository)
      words      <- Scenario.eval(wordRepository.getAllWordsByDictionary(chat.id, dictionary.id))
      result <-
        if (words.nonEmpty) Scenario.pure[Task](words)
        else
          Scenario.eval(
            EmptyDictionary(dictionary.name).sendTo(chat),
          ) >> provideWords(chat, dictionaryRepository, wordRepository)
    } yield result

  def provideOneWord(chat: Chat, dictionary: Dictionary, wordRepository: WordRepository)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, Word] =
    for {
      answer <- Scenario.expect(plainText)
      word <- Scenario.eval(wordRepository.getWordByName(chat.id, dictionary.id, answer)).attempt.flatMap {
        case Right(w) => Scenario.pure(w)
        case Left(WordByNameNotFound(w)) =>
          Scenario.eval(MissingWord(w).sendTo(chat)) >> provideOneWord(chat, dictionary, wordRepository)
        case _ => defaultErrorScenario(chat) >> provideOneWord(chat, dictionary, wordRepository)
      }
    } yield word

  def checkTextLength(chat: Chat, length: Int)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, String] =
    for {
      name <- Scenario.expect(plainText)
      result <-
        if (name.length <= length) Scenario.pure[Task](name)
        else
          Scenario.eval(
            LongText(length).sendTo(chat),
          ) >> checkTextLength(chat, length)
    } yield result

  def repeatWord(chat: Chat, word: Word)(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      _ <- Scenario.eval(AskAnswer(word.definition).sendTo(chat))
      _ <- checkAnswer(chat, word)
    } yield ()

  def checkAnswer(chat: Chat, word: Word)(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      answer <- Scenario.expect(plainText)
      _ <-
        if (answer.equalsIgnoreCase(word.word)) Scenario.eval(chat.send(Random.shuffle(Congrats).head))
        else if (answer.equalsIgnoreCase(ShowResult))
          Scenario.eval(chat.send(TextContent(text = displayWordMarkdown(word), parseMode = Some(Markdown))))
        else
          Scenario.eval(
            IncorrectAnswer.sendTo(chat, createKeyboardWithSingleButton(ShowResult)),
          ) >> checkAnswer(chat, word)
    } yield ()

  def sendMessage(chat: Chat, string: String)(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      _ <-
        if (string.length >= MaxMessageLength)
          Scenario.eval(GeneralMessage(string.slice(0, MaxMessageLength - 1)).sendTo(chat)) >> sendMessage(
            chat,
            string.slice(MaxMessageLength - 1, string.length),
          )
        else Scenario.eval(GeneralMessage(string).sendTo(chat))
    } yield ()

  def createKeyboardWithSingleButton(text: String): Keyboard.Reply = {
    val button = KeyboardButton.text(text)
    Keyboard.Reply(ReplyKeyboardMarkup.singleButton(button))
  }

  def displayWordMarkdown(word: Word): String =
    s"""
       |*${word.word}*
       |
       |${word.definition}
       |_${word.example.getOrElse("")}_
       |""".stripMargin

  private def fetchDictionaryName(chat: Chat, repository: DictionaryRepository)(implicit
      tc: TelegramClient[Task],
  ): Scenario[Task, String] = {
    def fetch(limit: Int, offset: Int): Scenario[Task, String] =
      for {
        dicts <- Scenario.eval(repository.getDictionaries(chat.id, offset, limit))
        _     <- Scenario.eval(chat.send("Choose a dictionary:", keyboard = dictionaryKeyboard(dicts, limit)))
        input <- Scenario.expect(plainText)
        result <- input match {
          case StartOverOption | MoreOption if dicts.size < limit => fetch(limit, 0)
          case StartOverOption | MoreOption                       => fetch(limit, offset + limit)
          case other                                              => Scenario.pure[Task](other)
        }
      } yield result

    def fetchOneKeyboard(dicts: List[Dictionary]): Scenario[Task, String] =
      for {
        _     <- Scenario.eval(chat.send("Choose a dictionary:", keyboard = dictionarySingleKeyboard(dicts)))
        input <- Scenario.expect(plainText)
      } yield input

    for {
      dicts <- Scenario.eval(repository.getAllDictionaries(chat.id))
      result <-
        if (dicts.size > 4) fetch(limit = 4, offset = 0)
        else fetchOneKeyboard(dicts)
    } yield result
  }

  private def dictionaryKeyboard(dicts: List[Dictionary], limit: Int): Keyboard.Reply = {
    def btn(dict: Dictionary): KeyboardButton = KeyboardButton.text(dict.name)

    val buttons = dicts.sliding(2, 2).toSeq.map(_.map(btn))
    Keyboard.Reply(
      ReplyKeyboardMarkup(
        if (dicts.size < limit) buttons :+ Seq(StartOverBtn)
        else buttons :+ Seq(MoreBtn),
      ),
    )
  }

  private def dictionarySingleKeyboard(dicts: List[Dictionary]): Keyboard.Reply = {
    def btn(dict: Dictionary): KeyboardButton = KeyboardButton.text(dict.name)

    val buttons = dicts.sliding(2, 2).toSeq.map(_.map(btn))
    Keyboard.Reply(
      ReplyKeyboardMarkup(buttons),
    )
  }

}
