package ru.dins.scalaschool.polyglot.client
import org.http4s.client.Client
import org.http4s.client.blaze.BlazeClientBuilder
import zio.interop.catz._
import zio.{Has, Task, TaskLayer, TaskManaged, URIO, ZIO}

object HttpClient {
  type HttpClient = Has[Client[Task]]

  val client: URIO[HttpClient, Client[Task]] = ZIO.service[Client[Task]]

  val live: TaskLayer[HttpClient] = createClient().toLayer

  private def createClient(): TaskManaged[Client[Task]] =
    ZIO
      .runtime[Any]
      .toManaged_
      .flatMap { implicit rt =>
        BlazeClientBuilder
          .apply[Task](rt.platform.executor.asEC)
          .resource
          .toManagedZIO
      }
}
