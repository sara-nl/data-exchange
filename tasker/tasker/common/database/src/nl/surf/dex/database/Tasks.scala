package nl.surf.dex.database

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._
import cats.implicits._
import nl.surf.dex.storage.Share.NePath

object Tasks {

  def insert(xa: Transactor[IO])(permision: Permission,
                                 datasetPath: NePath): IO[Int] = {
    sql"""
         |INSERT INTO surfsara_task
         |("state", "author_email", "approver_email", "algorithm", "algorithm_storage", "dataset", "dataset_storage", "review_output", "permission_id", "registered_on", "updated_on") VALUES 
         |('running', ${permision.algorithmProvider}, ${permision.datasetProvider}, ${permision.algorithm.path.segments
           .mkString_("/")}, ${permision.algorithm.storage.id}, ${datasetPath.segments.mkString_(
           "/"
         )}, ${permision.dataSet.storage.id}, false, ${permision.id}, current_timestamp, current_timestamp)
         |""".stripMargin.update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
  }

}
