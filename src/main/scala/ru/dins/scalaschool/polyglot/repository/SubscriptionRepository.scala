package ru.dins.scalaschool.polyglot.repository

import doobie.implicits.toSqlInterpolator
import doobie.util.transactor.Transactor
import zio.{Task, URIO}
import zio.interop.catz._
import doobie.implicits._
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import ru.dins.scalaschool.polyglot.db.Database
import ru.dins.scalaschool.polyglot.db.Database.DBTransactor
import ru.dins.scalaschool.polyglot.models.{Subscription, SubscriptionAlreadyExists, SubscriptionNotFound}
import doobie.postgres.implicits._

import java.time.LocalDateTime

trait SubscriptionRepository {
  def addSubscription(chatId: Long): Task[Subscription]
  def getSubscriptionByChatId(chatId: Long): Task[Subscription]
  def toggleNotifications(chatId: Long): Task[Subscription]
  def changeInterval(chatId: Long, interval: Long): Task[Subscription]
  def getActiveSubscriptions: Task[List[Subscription]]
  def recordNotification(chatId: Long): Task[Unit]
}

object SubscriptionRepository {
  def make(): URIO[DBTransactor, SubscriptionRepository] =
    for {
      transactor <- Database.transactor
    } yield new SubscriptionRepositoryImpl(transactor)
}

final private class SubscriptionRepositoryImpl(xa: Transactor[Task]) extends SubscriptionRepository {

  override def addSubscription(chatId: Long): Task[Subscription] =
    sql"insert into subscriptions(chat_id) values ($chatId)".update
      .withUniqueGeneratedKeys[Subscription]("id", "chat_id", "is_active", "interval", "last_notified")
      .attemptSomeSqlState { case UNIQUE_VIOLATION =>
        SubscriptionAlreadyExists(chatId)
      }
      .transact(xa)
      .absolve

  override def toggleNotifications(chatId: Long): Task[Subscription] =
    sql"update subscriptions set is_active = NOT is_active where chat_id = $chatId returning *"
      .query[Subscription]
      .option
      .transact(xa)
      .some
      .mapError(_ => SubscriptionNotFound(chatId))

  override def changeInterval(chatId: Long, interval: Long): Task[Subscription] =
    sql"update subscriptions set interval = $interval where chat_id = $chatId returning *"
      .query[Subscription]
      .option
      .transact(xa)
      .some
      .mapError(_ => SubscriptionNotFound(chatId))

  override def getSubscriptionByChatId(chatId: Long): Task[Subscription] =
    sql"select * from subscriptions where chat_id = $chatId"
      .query[Subscription]
      .option
      .transact(xa)
      .someOrFail(SubscriptionNotFound(chatId))

  override def getActiveSubscriptions: Task[List[Subscription]] =
    sql"select * from subscriptions where is_active = true"
      .query[Subscription]
      .to[List]
      .transact(xa)

  override def recordNotification(chatId: Long): Task[Unit] =
    sql"UPDATE subscriptions SET last_notified = ${LocalDateTime.now()} WHERE chat_id = $chatId".update.run
      .transact(xa)
      .unit
}
