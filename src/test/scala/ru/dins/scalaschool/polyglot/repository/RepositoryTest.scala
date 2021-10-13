package ru.dins.scalaschool.polyglot.repository

import ru.dins.scalaschool.polyglot.FlywayMigration
import ru.dins.scalaschool.polyglot.configuration.Config
import ru.dins.scalaschool.polyglot.db.Database
import ru.dins.scalaschool.polyglot.models.{WordAlreadyExists, WordContainer, WordNotFound}
import zio.blocking.Blocking
import zio.test.Assertion.{equalTo, isLeft, isNone}
import zio.test.environment.TestEnvironment
import zio.test.{DefaultRunnableSpec, TestFailure, ZSpec, assert}
import zio.Cause

object RepositoryTest extends DefaultRunnableSpec {

  val mockWordWithExample: WordContainer = WordContainer(
    1,
    1,
    "Polyglot",
    "knowing or using several languages",
    Some("Once upon a time..."),
  )

  val mockWordWithoutExample: WordContainer = mockWordWithExample.copy(example = None)

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("NotesStorage Live integration test")(
      testM("should add new word") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          word                 <- repository.addWord(mockWordWithExample)
        } yield assert(word.chatId)(equalTo(mockWordWithExample.chatId)) &&
          assert(word.word)(equalTo(mockWordWithExample.word)) &&
          assert(word.definition)(equalTo(mockWordWithExample.definition)) &&
          assert(word.example)(equalTo(mockWordWithExample.example))
      },
      testM("should add new word without example") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          word <- repository.addWord(
            mockWordWithoutExample,
          )
        } yield assert(word.chatId)(equalTo(mockWordWithExample.chatId)) &&
          assert(word.word)(equalTo(mockWordWithExample.word)) &&
          assert(word.definition)(equalTo(mockWordWithExample.definition)) &&
          assert(word.example)(isNone)
      },
      testM("should return error when word already exists") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          _                    <- repository.addWord(mockWordWithExample)
          secondWord           <- repository.addWord(mockWordWithExample).either
        } yield assert(secondWord)(isLeft(equalTo(WordAlreadyExists(mockWordWithExample.word))))
      },
      testM("should get word by id") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          word <- repository.addWord(
            mockWordWithoutExample,
          )
          result <- repository.getWordById(word.id)
        } yield assert(result)(equalTo(word))
      },
      testM("should return error if word not found") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          result               <- repository.getWordById(42).either
        } yield assert(result)(isLeft(equalTo(WordNotFound(42))))
      },
      testM("should return list of words") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          firstWord            <- repository.addWord(mockWordWithExample)
          secondWord <- repository.addWord(
            mockWordWithoutExample,
          )
          result <- repository.getAllWords(mockWordWithExample.chatId)
        } yield assert(result)(equalTo(List(firstWord, secondWord)))
      },
      testM("should update word") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          word                 <- repository.addWord(mockWordWithExample)
          updatedWord <- repository.updateWord(
            word.id,
            Some("Polyglot"),
            Some("Knowing or using several languages."),
            None,
          )
        } yield assert(updatedWord.word)(equalTo("Polyglot")) &&
          assert(updatedWord.definition)(equalTo("Knowing or using several languages.")) &&
          assert(updatedWord.example)(equalTo(mockWordWithExample.example))
      },
      testM("should return error if word to update not found") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          result <- repository
            .updateWord(42, Some("Polyglot"), Some("Knowing or using several languages."), None)
            .either
        } yield assert(result)(isLeft(equalTo(WordNotFound(42))))
      },
      testM("should delete word") {
        for {
          config               <- Config.get
          _                    <- FlywayMigration.migrate(config)
          dictionaryRepository <- DictionaryRepository.make()
          _                    <- dictionaryRepository.createDictionary(mockWordWithExample.chatId, "Quenya")
          repository           <- WordRepository.make()
          word                 <- repository.addWord(mockWordWithExample)
          isDeleted            <- repository.deleteWordById(word.id)
          search               <- repository.getWordById(word.id).either
        } yield assert(isDeleted)(equalTo(true)) &&
          assert(search)(isLeft(equalTo(WordNotFound(word.id))))
      },
    ).provideSomeLayer[TestEnvironment](
      (Blocking.live >+> TestContainer.postgresLayer >+> Database.live)
        .mapError(err => TestFailure.Runtime(Cause.die(new Exception(err)))),
    )
}
