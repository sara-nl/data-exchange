package watcher

import cats.effect._
import doobie._
import doobie.implicits._
import tasker.queue.Messages.ETag

object Task {

  def insert(
    xa: Transactor[IO]
  )(permision: Permission, datasetPath: String, eTag: ETag): IO[Int] = {
    val algorithmPath = permision.algorithmPath.userPath.getOrElse("/")
    sql"""
         |INSERT INTO "surfsara_task" 
         |("state", "author_email", "approver_email", "algorithm", "dataset", "review_output", "permission_id", "registered_on", "updated_on") VALUES 
         |('running', ${permision.algorithmProvider}, ${permision.datasetProvider}, $algorithmPath, $datasetPath, false, ${permision.id}, current_timestamp, current_timestamp)
         |""".stripMargin.update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
  }

}
