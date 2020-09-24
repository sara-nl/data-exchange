package watcher

import cats.effect.{IO, Resource}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.database.Permission
import nl.surf.dex.database.Permissions.PermissionWithRuns
import nl.surf.dex.storage.Share.NePath
import nl.surf.dex.storage.owncloud.WebdavPath
import nl.surf.dex.storage.{CloudStorage, FilesetOps}

case class DataSet(path: WebdavPath)

object DataSet {

  private val logger = Slf4jLogger.getLogger[IO]

  def newDatasetsPipe(
      filesetOpsResFactory: CloudStorage => Resource[IO, FilesetOps]
  ): fs2.Pipe[IO, PermissionWithRuns, (NePath, Permission)] =
    (_: fs2.Stream[IO, PermissionWithRuns]).flatMap {
      case (permission, handledDatasetLocations) =>
        fs2.Stream.evalSeq(
          filesetOpsResFactory(permission.dataSet.storage)
            .use(
              _.listShareFolder(permission.dataSet.path)
                .map(
                  _.toSet.diff(handledDatasetLocations.map(_.path).toSet).toList
                )
                .handleErrorWith { t =>
                  logger.error(t)(
                    FilesetOps.errors.notFound(permission.dataSet)
                  ) *> IO.pure(Nil)
                }
            )
        ) map { (_, permission) }

    }
}
