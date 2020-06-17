package nl.surf.dex.database

import cats.data.Kleisli
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Encoder
import io.circe.syntax._

object Permissions {

  type PermissionWithRuns = (Permission, List[AlgorithmRun2])

  // Queries
  private val allWithRunsQ =
    sql"""SELECT permission.id, 
         | permission.algorithm_provider, 
         | permission.dataset_provider, 
         | permission.algorithm, 
         | permission.algorithm_etag, 
         | permission.dataset, 
         | task.dataset
         |FROM surfsara_permission as permission
         |LEFT JOIN surfsara_task AS task
         |  ON permission.id = task.permission_id
         |WHERE permission.state = 'active' 
         |  AND permission.permission_type = 'stream permission'""".stripMargin

  private type AllWithRunsQR =
    (Int, String, String, String, String, String, Option[String])

  // Functions
  def allWithRuns(): Kleisli[IO, Transactor[IO], List[PermissionWithRuns]] =
    Kleisli { transactor =>
      for {
        queryResults <- allWithRunsQ
          .query[AllWithRunsQR]
          .stream
          .compile
          .toList
          .transact(transactor)
      } yield {
        queryResults.map {
          case (
              permissionId,
              algorithmProvider,
              datasetProvider,
              algorithm,
              algorithmETag,
              permissionDataset,
              taskDatasetOption
              ) =>
            (
              Permission(
                permissionId,
                algorithmProvider,
                datasetProvider,
                algorithm,
                algorithmETag,
                permissionDataset
              ),
              taskDatasetOption.map(AlgorithmRun2.apply)
            )
        }
      }.groupMapReduce(key = _._1)(_._2.toList)(_ ::: _).toList
    }

  def algorithmPath(id: Int): Kleisli[IO, Transactor[IO], String] =
    Kleisli { transactor =>
      sql"SELECT algorithm FROM surfsara_permission WHERE id = $id"
        .query[String]
        .unique
        .transact(transactor)
    }

  def updateStats[S: Encoder](id: Int,
                              eTag: String,
                              stats: S): Kleisli[IO, Transactor[IO], Int] = {
    Kleisli { transactor =>
      import xdoobie.jsonPut
      sql"""UPDATE surfsara_permission 
            | SET algorithm_report = ${stats.asJson},
            |  algorithm_etag = $eTag,
            |  state = ${Permission.State.pending.toString}
            | WHERE id = $id""".stripMargin.update.run
        .transact(transactor)
    }
  }

}
