package nl.surf.dex.database

import cats.data.Kleisli
import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import io.circe.Encoder
import io.circe.syntax._
import nl.surf.dex.storage.Share
import cats.implicits._
import nl.surf.dex.storage.Fileset.Hash
import nl.surf.dex.storage.Share.Location
object Permissions {

  type PermissionWithRuns = (Permission, List[Share.Location])

  // Queries
  private val activeStreamingWithRunsQ =
    sql"""SELECT permission.id, 
         | permission.algorithm_provider, 
         | permission.dataset_provider, 
         | permission.algorithm, 
         | permission.algorithm_storage, 
         | permission.algorithm_etag, 
         | permission.dataset, 
         | permission.dataset_storage, 
         | task.dataset,
         | task.dataset_storage
         |FROM surfsara_permission as permission
         |LEFT JOIN surfsara_task AS task
         |  ON permission.id = task.permission_id
         |WHERE permission.state = 'active' 
         |  AND permission.permission_type = 'stream permission'""".stripMargin

  private type ActiveStreamingWithRunsQR =
    (Int, String, String, String, String, String, String, String, Option[String], Option[String])

  // Functions
  def activeStreamingWithRuns: Kleisli[IO, Transactor[IO], List[PermissionWithRuns]] =
    Kleisli { transactor =>
      for {
        queryResults <-
          activeStreamingWithRunsQ
            .query[ActiveStreamingWithRunsQR]
            .stream
            .compile
            .toList
            .transact(transactor)
        permissionsWithRuns <- queryResults.traverse {
          case (
                permissionId,
                algorithmProvider,
                datasetProvider,
                algorithm,
                algorithmStorage,
                algorithmETag,
                permissionDataset,
                permissionDatasetStorage,
                taskDatasetOption,
                taskDatasetStorageOption
              ) =>
            for {
              algorithmLocation <- Location.parseIO(algorithmStorage, algorithm)
              datasetLocation <- Location.parseIO(
                permissionDatasetStorage,
                permissionDataset
              )
              taskDatasetLocation <- (
                  taskDatasetStorageOption,
                  taskDatasetOption
              ).tupled.traverse {
                Function.tupled(Location.parseIO)
              }
            } yield (
              Permission(
                permissionId,
                algorithmProvider,
                datasetProvider,
                algorithmLocation,
                algorithmETag,
                datasetLocation
              ),
              taskDatasetLocation
            )
        }
      } yield permissionsWithRuns
        .groupMapReduce(key = _._1)(_._2.toList)(_ ::: _)
        .toList
    }

  def algorithmLocation(id: Int): Kleisli[IO, Transactor[IO], Share.Location] =
    Kleisli { transactor =>
      sql"SELECT algorithm_storage, algorithm FROM surfsara_permission WHERE id = $id"
        .query[(String, String)]
        .unique
        .transact(transactor)
        .flatMap(Function.tupled(Location.parseIO))
    }

  def updateStats[S: Encoder](id: Int, hash: Hash, stats: S): Kleisli[IO, Transactor[IO], Int] = {
    Kleisli { transactor =>
      import xdoobie.jsonPut
      sql"""UPDATE surfsara_permission 
            | SET algorithm_report = ${stats.asJson},
            |  algorithm_etag = ${hash.value},
            |  state = ${Permission.State.pending.toString}
            | WHERE id = $id""".stripMargin.update.run
        .transact(transactor)
    }
  }

}
