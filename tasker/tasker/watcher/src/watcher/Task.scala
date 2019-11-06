package watcher

import cats.effect._
import doobie._
import doobie.implicits._

object Task {

  def insert(xa: Transactor[IO])(permision: Permission,
                                 datasetPath: String): IO[Int] = {
    val algorithmPath = permision.algorithmPath.userPath.getOrElse("/")
    sql"""
         |INSERT INTO "surfsara_task" 
         |("state", "author_email", "approver_email", "output", "registered_on", "algorithm", "dataset", "dataset_desc", "algorithm_content", "review_output", "permission_id", "algorithm_etag", "algorithm_info") VALUES 
         |('running', ${permision.algorithmProvider}, ${permision.datasetProvider}, '', current_timestamp, ${algorithmPath}, $datasetPath, '', '[]', false, ${permision.id}, 'xxx', '{}')
         |""".stripMargin.update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
  }

}
