package ru.dins.scalaschool.polyglot.models

import java.time.LocalDateTime
import java.util.UUID

final case class Word(
    id: Long,
    chatId: Long,
    dictionaryId: Long,
    word: String,
    definition: String,
    example: Option[String],
    createdAt: LocalDateTime,
)

final case class Dictionary(
    id: Long,
    name: String,
    chatId: Long,
    createdAt: LocalDateTime,
)

final case class Subscription(
    id: Long,
    chatId: Long,
    isActive: Boolean,
    interval: Long,
    lastNotified: LocalDateTime,
)

final case class WordContainer(
    chatId: Long,
    dictionaryId: Long,
    word: String,
    definition: String,
    example: Option[String],
)

final case class WordsList(chatId: Long, words: List[Word])

final case class WordNotFound(id: Long) extends Exception

final case class SubscriptionNotFound(chatId: Long) extends Exception

final case class WordByNameNotFound(name: String) extends Exception

final case class WordAlreadyExists(word: String) extends Exception

final case class SubscriptionAlreadyExists(chatId: Long) extends Exception

final case class UnexpectedError(message: String = "Unexpected error") extends Exception

final case class DictionaryAlreadyExists(name: String) extends Exception

final case class DictionaryNotFound(name: String) extends Exception

final case class User(
    id: UUID,
    chatId: Long,
)
