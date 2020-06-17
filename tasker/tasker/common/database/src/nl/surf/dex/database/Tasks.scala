package nl.surf.dex.database

import cats.effect.IO
import doobie.Transactor
import doobie.implicits._

object Tasks {

  def insert(xa: Transactor[IO])(permision: Permission,
                                 datasetPath: String): IO[Int] = {
    sql"""
         |INSERT INTO surfsara_task
         |("state", "author_email", "approver_email", "algorithm", "dataset", "review_output", "permission_id", "registered_on", "updated_on") VALUES 
         |('running', ${permision.algorithmProvider}, ${permision.datasetProvider}, ${permision.algorithmPath}, $datasetPath, false, ${permision.id}, current_timestamp, current_timestamp)
         |""".stripMargin.update
      .withUniqueGeneratedKeys[Int]("id")
      .transact(xa)
  }

}
