package tasker.webdav

import java.io.InputStream
import java.nio.file.{Files, Path, Paths}

import cats.effect.{Concurrent, IO, Resource}
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import fs2.Pipe
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.config.CommonConf
import tasker.config.CommonConf.ResearchDriveConf
import tasker.queue.Messages.ETag

import scala.jdk.CollectionConverters._

object Webdav {

  def makeClient: IO[Webdav] =
    for {
      rdConf <- CommonConf.loadF.map(_.researchDrive)
      sardine <- IO(
        SardineFactory.begin(rdConf.webdavUsername, rdConf.webdavPassword)
      )
    } yield Webdav(sardine, rdConf)

  object implicits {
    implicit class DavResourceWithConversions(val r: DavResource)
        extends AnyVal {
      def getSafeETag: ETag =
        ETag(r.getEtag.stripSuffix("\"").stripPrefix("\""))
    }
  }
}

case class Webdav(sardine: Sardine, rdConf: ResearchDriveConf) {

  private val downloadFilesPipe: Pipe[IO, (WebdavPath, Path), Unit] = {
    msgStream =>
      for {
        paths <- msgStream
        (webdavRoot, localRoot) = paths
        _ <- findAll(webdavRoot).evalMap {
          case (remotePath, isResource) =>
            val localPath =
              Paths.get(localRoot.toString, remotePath.userPath.get.toString)
            for {
              logger <- Slf4jLogger.create[IO]
              _ <- logger.info(
                s"Copying remote file ${remotePath.toString} to local: ${localPath.toString}"
              )
              _ <- isResource.use(copyStreamToLocalFile(_, localPath))
            } yield ()
        }
      } yield ()
  }

  /**
    * Lists path contents (single level).
    */
  def list(path: WebdavPath): IO[List[DavResource]] = IO {
    sardine
      .list(path.toURI.toString)
      .asScala
      .toList
  }

  def findAll(
    webdavRoot: WebdavPath
  ): fs2.Stream[IO, (WebdavPath, Resource[IO, InputStream])] = {
    for {
      davResources <- fs2.Stream.eval(
        IO(
          sardine
            .list(webdavRoot.toURI.toString, rdConf.maxFolderDepth)
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
  def downloadToHost(
    map: Map[WebdavPath, Path]
  )(implicit F: Concurrent[IO]): IO[Unit] =
    fs2.Stream
      .emits[IO, (WebdavPath, Path)](map.view.toList)
      .balanceThrough(4)(downloadFilesPipe)
      .compile
      .drain

  private def copyStreamToLocalFile(is: InputStream,
                                    localPath: Path): IO[Unit] = IO {
    localPath.toFile.getParentFile.mkdirs()
    Files.copy(is, localPath)
    ()
  }

}
