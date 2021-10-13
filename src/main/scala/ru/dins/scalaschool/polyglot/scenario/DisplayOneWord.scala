package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.models.ParseMode.Markdown
import canoe.models.outgoing.TextContent
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.{AskDictionaryName, AskForWord}
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, WordRepository}
import zio.Task
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand

final class DisplayOneWord(
    wordRepository: WordRepository,
    dictionaryRepository: DictionaryRepository,
) extends PolyglotScenario {
  override def chatCommand: PolyglotCommand = PolyglotCommand("oneword", "Display one word.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat       <- Scenario.expect(command(chatCommand.tag).chat)
      dictionary <- provideDictionaryByName(chat, dictionaryRepository)
      _          <- Scenario.eval(AskForWord.sendTo(chat))
      word       <- provideOneWord(chat, dictionary, wordRepository)
      _          <- Scenario.eval(chat.send(TextContent(text = displayWordMarkdown(word), parseMode = Some(Markdown))))
    } yield ()
}

object DisplayOneWord {
  def apply(wordRepository: WordRepository, dictionaryRepository: DictionaryRepository) =
    new DisplayOneWord(wordRepository, dictionaryRepository)
}
