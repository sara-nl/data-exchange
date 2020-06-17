package watcher

import cats.effect.IO
import doobie.implicits._
import doobie.util.transactor.Transactor
import nl.surf.dex.database.Permission

object Permissions {

  case class AlgorithmRun(dataSetPath: String)

  type PermissionWithRuns = (Permission, List[AlgorithmRun])

  type QueryResult =
    (Int, String, String, String, String, String, Option[String])

  private val query =
    sql"""SELECT permission.id, permission.algorithm_provider, permission.dataset_provider, permission.algorithm, permission.algorithm_etag, permission.dataset, task.dataset
                   |  FROM surfsara_permission as permission LEFT JOIN surfsara_task as task ON permission.id = task.permission_id
                   |  WHERE permission.state = 'active' AND permission.permission_type = 'stream permission'""".stripMargin

  /**
    * Fetches permissions of type streaming from the DB along with happened algorithm runs
    */
  def findAllPermissions(xa: Transactor[IO]): IO[List[PermissionWithRuns]] =
    for {
      queryResults <- query
        .query[QueryResult]
        .stream
        .compile
        .toList
        .transact(xa)
    } yield {
      val transformedResults = queryResults.map {
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
            taskDatasetOption.map(taskDataset => AlgorithmRun(taskDataset))
          )
      }

      transformedResults
        .groupMapReduce(key = _._1)(_._2.toList)(_ ::: _)
        .toList
    }

}
