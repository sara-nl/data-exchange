package watcher

import cats.effect.IO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.webdav.{Webdav, WebdavPath}
import cats.implicits._
import WebdavPath.implicits._
import tasker.queue.Messages.ETag
import watcher.Permission.PermissionWithRuns

case class DataSet(path: WebdavPath)

object DataSet {

  private val logger = Slf4jLogger.getLogger[IO]

  val newDatasetsPipe
    : fs2.Pipe[IO, PermissionWithRuns, (WebdavPath, ETag, Permission)] =
    _.flatMap {
      case (permission, permissionRuns) =>
        val oldDataSetPaths = permissionRuns.map(_.dataSetPath)
        fs2.Stream
          .evalSeq({
            Webdav
              .list(permission.dataSetPath)
              .handleErrorWith { t =>
                logger.error(t)(
                  s"Could not retrieve resource ${permission.dataSetPath}. Skipping."
                )
                IO(Nil)
              }
              .flatMap {
                case resources if resources.length > 1 =>
                  IO(
                    resources.tail.map(
                      resource =>
                        (
                          WebdavPath(resource),
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
          .filter(path => !oldDataSetPaths.contains(path))
          .filter(_._1 =!= permission.dataSetPath)
    }
}
