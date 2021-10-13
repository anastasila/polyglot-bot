package ru.dins.scalaschool.polyglot.scenario

import canoe.api._
import canoe.syntax._
import ru.dins.scalaschool.polyglot.models.ChatMessage.NoDictionaries
import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import ru.dins.scalaschool.polyglot.models.Dictionary
import ru.dins.scalaschool.polyglot.repository.DictionaryRepository
import zio._

final class DisplayDictionariesScenario(dictionaryRepository: DictionaryRepository) extends PolyglotScenario {

  import DisplayDictionariesScenario._

  override val chatCommand: PolyglotCommand = PolyglotCommand("dictionaries", "Get list of all dictionaries.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat         <- Scenario.expect(command(chatCommand.tag).chat)
      dictionaries <- Scenario.eval(dictionaryRepository.getAllDictionaries(chat.id))
      _ <-
        if (dictionaries.nonEmpty) sendMessage(chat, displayDictionaries(dictionaries))
        else Scenario.eval(NoDictionaries.sendTo(chat))
    } yield ()
}

object DisplayDictionariesScenario {

  private def displayDictionaries(dictionaries: List[Dictionary]): String =
    s"${dictionaries.size} dictionaries: \n" +
      dictionaries.map(s => s"— ${s.name}").mkString("\n")

  def apply(dictionaryRepository: DictionaryRepository): DisplayDictionariesScenario =
    new DisplayDictionariesScenario(dictionaryRepository)
}
