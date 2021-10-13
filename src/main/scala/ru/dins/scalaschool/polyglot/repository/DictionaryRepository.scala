package ru.dins.scalaschool.polyglot.repository

import zio.{Task, URIO}
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import zio.interop.catz._
import ru.dins.scalaschool.polyglot.db.Database
import ru.dins.scalaschool.polyglot.db.Database.DBTransactor
import ru.dins.scalaschool.polyglot.models.{Dictionary, DictionaryAlreadyExists, DictionaryNotFound}

trait DictionaryRepository {
  def createDictionary(chatId: Long, dictionaryName: String): Task[Dictionary]
  def getDictionaryByName(dictionaryName: String): Task[Dictionary]
  def getAllDictionaries(chatId: Long): Task[List[Dictionary]]
  def getDictionaries(chatId: Long, offset: Int, limit: Int): Task[List[Dictionary]]
}

object DictionaryRepository {
  def make(): URIO[DBTransactor, DictionaryRepository] =
    for {
      transactor <- Database.transactor
    } yield new DictionaryRepositoryImpl(transactor)

  final private class DictionaryRepositoryImpl(xa: Transactor[Task]) extends DictionaryRepository {
    override def createDictionary(chatId: Long, dictionaryName: String): Task[Dictionary] =
      sql"INSERT INTO dictionary (name, chat_id) VALUES ($dictionaryName, $chatId)".update
        .withUniqueGeneratedKeys[Dictionary]("id", "name", "chat_id", "created_at")
        .attemptSomeSqlState { case UNIQUE_VIOLATION => DictionaryAlreadyExists(dictionaryName) }
        .transact(xa)
        .absolve

    override def getDictionaryByName(dictionaryName: String): Task[Dictionary] =
      sql"select * from dictionary where name = $dictionaryName"
        .query[Dictionary]
        .option
        .transact(xa)
        .someOrFail(DictionaryNotFound(dictionaryName))

    override def getAllDictionaries(chatId: Long): Task[List[Dictionary]] =
      sql"select * from dictionary where chat_id = $chatId"
        .query[Dictionary]
        .to[List]
        .transact(xa)

    override def getDictionaries(chatId: Long, offset: Int, limit: Int): Task[List[Dictionary]] =
      sql"select * from dictionary where chat_id = $chatId ORDER BY id LIMIT $limit OFFSET $offset"
        .query[Dictionary]
        .to[List]
        .transact(xa)
  }
}
