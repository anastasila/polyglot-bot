package ru.dins.scalaschool.polyglot.scenario

import ru.dins.scalaschool.polyglot.scenario.PolyglotScenario.PolyglotCommand
import canoe.api._
import canoe.syntax._
import zio._

final class HelpScenario(commands: List[PolyglotCommand]) extends PolyglotScenario {

  import HelpScenario._

  private val helpMessage = composeHelpMessage(commands)

  override val chatCommand: PolyglotCommand = PolyglotCommand("start", "Display user help message.")

  override def scenario(implicit tc: TelegramClient[Task]): Scenario[Task, Unit] =
    for {
      chat <- Scenario.expect(command(chatCommand.tag).chat)
      _    <- Scenario.eval(chat.send(helpMessage))
    } yield ()
}

object HelpScenario {

  private val Help =
    """
      |Hello! My name is Polyglot!
      |
      |I will help you memorize words.
      |
      |Available operations:""".stripMargin

  private def composeHelpMessage(commands: List[PolyglotCommand]): String =
    Help + commands.map(c => s"/${c.tag} - ${c.description}").mkString("\n", "\n", "")

  def apply(commands: List[PolyglotCommand]): HelpScenario = new HelpScenario(commands)
}
