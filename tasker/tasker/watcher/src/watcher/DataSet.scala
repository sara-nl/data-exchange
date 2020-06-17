package watcher

import cats.effect.IO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.implicits._
import nl.surf.dex.database.Permission
import nl.surf.dex.storage.owncloud.implicits._
import nl.surf.dex.storage.ETag
import nl.surf.dex.storage.owncloud.{Webdav, WebdavPath}
import watcher.Permissions.PermissionWithRuns

case class DataSet(path: WebdavPath)

object DataSet {

  private val logger = Slf4jLogger.getLogger[IO]

  def newDatasetsPipe(
    webdavBase: WebdavPath
  ): fs2.Pipe[IO, PermissionWithRuns, (WebdavPath, ETag, Permission)] =
    _.flatMap {
      case (permission, permissionRuns) =>
        val dataSetPath = webdavBase.change(permission.dataSetPath)
        val oldDataSetPaths =
          permissionRuns.map(run => webdavBase.change(run.dataSetPath))
        fs2.Stream
          .evalSeq({
            Webdav.makeClient
              .flatMap(
                _.list(dataSetPath)
                  .handleErrorWith { t =>
                    logger.error(t)(
                      s"Could not retrieve resource ${permission.dataSetPath}. Skipping."
                    )
                    IO(Nil)
                  }
              )
              .flatMap {
                case resources if resources.length > 1 =>
                  IO(
                    resources.tail.map(
                      resource =>
                        (
                          webdavBase.change(resource),
                          ETag(resource.getEtag),
                          permission
                      )
                    )
                  )
                case resources if resources.length == 1 =>
                  logger.error(
                    s"Path ${permission.dataSetPath} is either empty or a file and can not be treated as a stream of data sets"
                  ) *> IO.pure(Nil)
                case _ =>
                  logger.error(
                    s"WebDav didn't return any resources for ${permission.dataSetPath}. Skipping."
                  ) *> IO.pure(Nil)
              }
          })
          .filter(path => !oldDataSetPaths.contains(path._1))
          .filter(_._1 =!= dataSetPath)
    }
}
