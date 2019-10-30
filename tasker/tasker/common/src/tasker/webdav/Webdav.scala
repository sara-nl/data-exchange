package tasker.webdav

import java.io.InputStream
import java.nio.file.{Files, Path, Paths}

import cats.effect.{IO, Resource}
import com.github.sardine.SardineFactory
import fs2.Pipe
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.config.TaskerConfig
import scala.jdk.CollectionConverters._

object Webdav {

  val sardine = SardineFactory.begin(
    TaskerConfig.webdav.username,
    TaskerConfig.webdav.password
  )

  private val downloadFilesPipe: Pipe[IO, (WebdavPath, Path), Unit] = {
    msgStream =>
      for {
        paths <- msgStream
        (webdavRoot, localRoot) = paths
        _ <- Webdav.findAll(webdavRoot).evalMap {
          case (remotePath, isResource) =>
            val localPath =
              Paths.get(localRoot.toString, remotePath.userPath.get.toString)
            for {
              logger <- Slf4jLogger.create[IO]
              _ <- logger.info(
                s"Copying remote file ${remotePath.toString} to local: ${localPath.toString}"
              )
              _ <- isResource.use(Webdav.copyStreamToLocalFile(_, localPath))
            } yield ()
        }
      } yield ()
  }

  def findAll(
    webdavRoot: WebdavPath
  ): fs2.Stream[IO, (WebdavPath, Resource[IO, InputStream])] = {
    for {
      davResources <- fs2.Stream.eval(
        IO(
          sardine
            .list(webdavRoot.toURI.toString, TaskerConfig.webdav.maxFolderDepth)
            .asScala
            .toList
        )
      )
      davResource <- fs2.Stream
        .fromIterator[IO](davResources.iterator)
        .filter(!_.isDirectory)
    } yield
      (
        webdavRoot.change(davResource),
        Resource.fromAutoCloseable[IO, InputStream](IO.delay {
          sardine.get(webdavRoot.change(davResource).toURI.toString)
        })
      )

  }

  /**
    * Downloads artifacts into their locations on the host.
    */
  def downloadToHost(map: Map[WebdavPath, Path]): IO[Unit] =
    fs2.Stream
      .emits(map.view.toList)
      .through(Webdav.downloadFilesPipe)
      .compile
      .drain

  private def copyStreamToLocalFile(is: InputStream,
                                    localPath: Path): IO[Unit] = IO {
    localPath.toFile.getParentFile.mkdirs()
    Files.copy(is, localPath)
    ()
  }

}
