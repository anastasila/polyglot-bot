package ru.dins.scalaschool.polyglot.models

import canoe.api._
import canoe.api.models.Keyboard
import canoe.syntax._
import canoe.models.Chat
import canoe.models.messages.TextMessage
import zio.Task

sealed abstract class ChatMessage(content: String) {
  def sendTo(chat: Chat, keyboard: Keyboard = Keyboard.Remove)(implicit tc: TelegramClient[Task]): Task[TextMessage] =
    chat.send(content, keyboard = keyboard)
}

object ChatMessage {
  case class GeneralMessage(content: String) extends ChatMessage(content)

  object UnexpectedError extends ChatMessage("Unexpected error encountered! :( Please try again later")

  object NoDictionaries extends ChatMessage("You don't have dictionaries yet. You can create /newdictionary")

  object AskDictionaryForNewWord extends ChatMessage("In which dictionary do you want to add new word?")

  object AskWord extends ChatMessage("What word do you want to remember?")

  object AskWordForUpdate extends ChatMessage("Which word do you want to update?")

  object AskDictionaryName extends ChatMessage("Enter a dictionary name:")

  object AskForWord extends ChatMessage("What word do you want to find?")

  case class AskInterval(interval: Long)
      extends ChatMessage(s"Current interval of notifications is $interval seconds. Enter new interval in seconds:")

  object AskExample
      extends ChatMessage("""Enter an example with this word or type "skip" to save word without example""")

  object AskNameForNewDictionary extends ChatMessage("Please enter a name for the new dictionary:")

  case class LongText(length: Int)
      extends ChatMessage(s"This text is too long. Please enter text less than $length character long:")

  object IncorrectAnswer
      extends ChatMessage("""Incorrect answer. Try again or press "Show result" to see the answer:""")

  case class AskWordDefinition(word: String) extends ChatMessage(s"What's the meaning of the word $word?")

  case class ExistingWord(word: String) extends ChatMessage(s"Word $word already exists. Please enter another word:")

  case class ExistingDictionary(name: String) extends ChatMessage(s"Dictionary with name $name already exists.")

  case class ToggleSubscription(isActive: Boolean)
      extends ChatMessage(s"Now your notifications are ${if (isActive) "on" else "off"}.")

  case class MissingSubscription(chatId: Long)
      extends ChatMessage(s"You don't have any dictionaries yet. To get notifications please create /newdictionary")

  case class MissingDictionary(name: String)
      extends ChatMessage(
        s"Dictionary with name $name not found. Please create /newdictionary or choose an existing dictionary:",
      )

  object WrongInterval
      extends ChatMessage(
        "Interval you provide is incorrect. Please enter a number.",
      )

  case class MissingWord(word: String)
      extends ChatMessage(
        s"Word $word not found. Please /addword or enter an existing word:",
      )

  case class EmptyDictionary(name: String)
      extends ChatMessage(
        s"Dictionary with name $name is empty. Please choose another dictionary.",
      )

  case class AskAnswer(definition: String)
      extends ChatMessage(
        s"What's the word for: \n $definition?",
      )

  case class SuccessfulRepeat(dictName: String)
      extends ChatMessage(
        s"Great job! You have repeated all words from the dictionary $dictName 🥳",
      )

  case class SuccessfulWordCreation(word: String, dictName: String)
      extends ChatMessage(
        s"Word $word was successfully added to the dictionary $dictName!",
      )

  case class SuccessfulDictionaryCreation(dictName: String)
      extends ChatMessage(
        s"Dictionary $dictName successfully created!",
      )

  case class SuccessfulIntervalChange(interval: Long)
      extends ChatMessage(
        s"Interval of notifications was successfully changed to $interval seconds!",
      )
}
