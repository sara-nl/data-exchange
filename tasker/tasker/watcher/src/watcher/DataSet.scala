package watcher

import cats.effect.IO
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.webdav.{Webdav, WebdavPath}
import cats.implicits._
import WebdavPath.implicits._

case class DataSet(path: WebdavPath)

object DataSet {

  private val logger = Slf4jLogger.getLogger[IO]

  val newDatasetsPipe: fs2.Pipe[IO, Unit, WebdavPath] = _.flatMap { _ =>
    fs2.Stream
      .evalSeq(Permission.findAllPermissions())
      .evalTap { permission =>
        logger.debug(s"Reacting on permission $permission")
      }
      .flatMap {
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
                    IO(resources.tail.map(WebdavPath.apply))
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
            .filter(_ =!= permission.dataSetPath)
      }
  }

}
