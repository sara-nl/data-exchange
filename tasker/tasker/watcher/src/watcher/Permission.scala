package watcher

import cats.effect.IO
import tasker.webdav.WebdavPath
import doobie.implicits._
import doobie.util.transactor.Transactor

case class Permission(id: Int,
                      algorithmProvider: String,
                      datasetProvider: String,
                      algorithmPath: WebdavPath,
                      algorithmETag: Option[String],
                      dataSetPath: WebdavPath)

object Permission {

  case class AlgorithmRun(dataSetPath: WebdavPath)

  type PermissionWithRuns = (Permission, List[AlgorithmRun])

  type QueryResult =
    (Int, String, String, String, Option[String], String, String)

  private val query =
    sql"""  SELECT permission.id, permission.algorithm_provider, permission.dataset_provider, permission.algorithm, permission.algorithm_etag, permission.dataset, task.dataset
                   |  FROM surfsara_permission as permission
                   |  LEFT JOIN surfsara_task as task ON permission.id = task.permission_id
                   |  WHERE permission.state != 'rejected' AND permission.permission_type = 'stream permission' AND permission.algorithm != 'Any algorithm'""".stripMargin

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
            taskDataset
            ) =>
          (
            Permission(
              permissionId,
              algorithmProvider,
              datasetProvider,
              WebdavPath(algorithm),
              algorithmETag,
              WebdavPath(permissionDataset)
            ),
            AlgorithmRun(WebdavPath(taskDataset))
          )
      }

      transformedResults.groupMap(_._1)(_._2).toList
    }

}
