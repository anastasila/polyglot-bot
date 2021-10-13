package ru.dins.scalaschool.polyglot.repository

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor.Transactor
import zio.{Task, URIO}
import zio.interop.catz._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.util.fragments.setOpt
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import ru.dins.scalaschool.polyglot.db.Database
import ru.dins.scalaschool.polyglot.db.Database.DBTransactor
import ru.dins.scalaschool.polyglot.models.{
  UnexpectedError,
  Word,
  WordAlreadyExists,
  WordByNameNotFound,
  WordContainer,
  WordNotFound,
}

trait WordRepository {
  def addWord(word: WordContainer): Task[Word]
  def getWordById(id: Long): Task[Word]
  def getWordByName(chatId: Long, dictId: Long, name: String): Task[Word]
  def getRandomWord(dictionaryId: Long): Task[Word]
  def getAllWords(chatId: Long): Task[List[Word]]
  def getAllWordsByDictionary(chatId: Long, dictionaryId: Long): Task[List[Word]]
  def updateWord(id: Long, word: Option[String], definition: Option[String], example: Option[String]): Task[Word]
  def deleteWordById(id: Long): Task[Boolean]
}

object WordRepository {
  def make(): URIO[DBTransactor, WordRepository] =
    for {
      transactor <- Database.transactor
    } yield new WordRepositoryImpl(transactor)
}

final private class WordRepositoryImpl(xa: Transactor[Task]) extends WordRepository {

  override def addWord(word: WordContainer): Task[Word] =
    sql"insert into words (chat_id, dictionary_id, word, definition, example) values (${word.chatId}, ${word.dictionaryId}, ${word.word}, ${word.definition}, ${word.example})".update
      .withUniqueGeneratedKeys[Word]("id", "chat_id", "dictionary_id", "word", "definition", "example", "created_at")
      .attemptSomeSqlState { case UNIQUE_VIOLATION =>
        WordAlreadyExists(word.word)
      }
      .transact(xa)
      .absolve

  override def getWordById(id: Long): Task[Word] =
    sql"select * from words where id = $id"
      .query[Word]
      .option
      .transact(xa)
      .someOrFail(WordNotFound(id))

  override def getRandomWord(dictionaryId: Long): Task[Word] =
    sql"select * from words where dictionary_id = $dictionaryId order by random() limit 1"
      .query[Word]
      .option
      .transact(xa)
      .someOrFail(UnexpectedError())

  override def getWordByName(chatId: Long, dictId: Long, name: String): Task[Word] =
    sql"select * from words where chat_id = $chatId and dictionary_id = $dictId and word = $name"
      .query[Word]
      .option
      .transact(xa)
      .someOrFail(WordByNameNotFound(name))

  override def getAllWords(chatId: Long): Task[List[Word]] =
    sql"select * from words where chat_id = $chatId".query[Word].to[List].transact(xa)

  override def getAllWordsByDictionary(chatId: Long, dictionaryId: Long): Task[List[Word]] =
    sql"select * from words where chat_id = $chatId and dictionary_id = $dictionaryId".query[Word].to[List].transact(xa)

  override def updateWord(
      id: Long,
      word: Option[String],
      definition: Option[String],
      example: Option[String],
  ): Task[Word] = {
    val f1 = word.map(s => fr"word = $s ")
    val f2 = definition.map(s => fr"definition = $s ")
    val f3 = example.map(s => fr"example = $s")
    val q = fr"update words" ++
      setOpt(f1, f2, f3) ++ fr"where id = $id returning *"
    q.query[Word]
      .option
      .transact(xa)
      .some
      .mapError(_ => WordNotFound(id))
  }

  override def deleteWordById(id: Long): Task[Boolean] =
    sql"delete from words where id = $id".update.run
      .transact(xa)
      .fold(_ => false, _ => true)
}
