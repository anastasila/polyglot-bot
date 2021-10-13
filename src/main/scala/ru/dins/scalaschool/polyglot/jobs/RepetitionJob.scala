package ru.dins.scalaschool.polyglot.jobs

import canoe.api.TelegramClient
import canoe.api.models.{ChatApi, Keyboard}
import canoe.models.outgoing.TextContent
import canoe.models.{InlineKeyboardButton, InlineKeyboardMarkup, PrivateChat}
import ru.dins.scalaschool.polyglot.models.{Dictionary, Subscription, Word}
import ru.dins.scalaschool.polyglot.repository.{DictionaryRepository, SubscriptionRepository, WordRepository}
import zio.clock.Clock
import zio.duration.durationInt
import zio.{Schedule, Task, ZIO}
import scala.util.Random
import java.time.LocalDateTime

final class RepetitionJob(
    wordRepository: WordRepository,
    dictionaryRepository: DictionaryRepository,
    subscriptionRepository: SubscriptionRepository,
) {

  import RepetitionJob._

  def sendRepetitionMessages(implicit tc: TelegramClient[Task]): ZIO[Any with Clock, Throwable, Unit] =
    (for {
      messages <- extractMessages()
      _        <- ZIO.foreach_(messages)(sendWord(_))
    } yield ()).repeat(Schedule.forever && Schedule.spaced(1.minute)).unit

  def extractMessages(): Task[List[(Long, List[Word])]] =
    for {
      activeSubscribers <- subscriptionRepository.getActiveSubscriptions
      messages          <- getMessages(activeSubscribers)
    } yield messages

  def sendWord(message: (Long, List[Word]))(implicit tc: TelegramClient[Task]): Task[Unit] = {
    val chatId = message._1
    val words  = message._2

    for {
      _ <- new ChatApi(PrivateChat(chatId, None, None, None))
        .send(
          content = TextContent(s"What's the word for: ${words.last.definition}?"),
          keyboard = createInlineKeyboard(words),
        )

      _ <- subscriptionRepository.recordNotification(chatId)
    } yield ()
  }

  def createInlineKeyboard(words: List[Word]): Keyboard.Inline = {
    val correctAnswer    = createButton(words.last, "\uD83D\uDC4D")
    val incorrectAnswers = words.take(3).map(word => createButton(word, "\uD83D\uDC4E"))
    val buttons          = correctAnswer :: incorrectAnswers
    Keyboard.Inline(InlineKeyboardMarkup.singleColumn(Random.shuffle(buttons)))
  }

  def createButton(word: Word, answer: String): InlineKeyboardButton =
    InlineKeyboardButton.callbackData(text = word.word, cbd = answer)

  private def getMessages(activeSubscribers: List[Subscription]): Task[List[(Long, List[Word])]] = {
    val fetchNotificationData = ZIO.foreach(activeSubscribers) { sub =>
      if (shouldBeNotified(sub)) {
        for {
          dictionaries <- dictionaryRepository.getAllDictionaries(sub.chatId)
          dictWords    <- ZIO.foreach(dictionaries)(getWords).map(_.flatten)
          wordList = Random.shuffle(dictWords).headOption
        } yield wordList.map(words => sub.chatId -> words)
      } else {
        ZIO.none
      }
    }

    fetchNotificationData.map(_.flatten)
  }

  private def getWords(dictionary: Dictionary): Task[Option[List[Word]]] =
    for {
      words <- wordRepository.getAllWordsByDictionary(dictionary.chatId, dictionary.id)
      res = if (words.length >= 4) Some(Random.shuffle(words).take(4)) else None
    } yield res
}

object RepetitionJob {

  def apply(
      wordRepository: WordRepository,
      dictionaryRepository: DictionaryRepository,
      subscriptionRepository: SubscriptionRepository,
  ): RepetitionJob = new RepetitionJob(wordRepository, dictionaryRepository, subscriptionRepository)

  private def shouldBeNotified(subscription: Subscription): Boolean =
    LocalDateTime.now().isAfter(subscription.lastNotified.plusSeconds(subscription.interval))
}
