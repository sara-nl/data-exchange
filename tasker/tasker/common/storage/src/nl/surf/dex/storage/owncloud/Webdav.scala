package nl.surf.dex.storage.owncloud

import java.io.{FileNotFoundException, InputStream}
import java.nio.file.{Files, Path}

import better.files.{File => BFile}
import cats.effect.{Concurrent, ContextShift, IO, Resource}
import cats.implicits._
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import fs2.Pipe
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.storage.ETag
import nl.surf.dex.storage.config.DexStorageConf
import nl.surf.dex.storage.config.DexStorageConf.ResearchDriveConf
import nl.surf.dex.storage.owncloud.Webdav.logger

import scala.jdk.CollectionConverters._

object Webdav {

  private val logger = Slf4jLogger.getLogger[IO]

  def makeClient: IO[Webdav] =
    for {
      conf <- DexStorageConf.loadF
      sardine <- IO(
        SardineFactory.begin(
          conf.researchDrive.webdavUsername,
          conf.researchDrive.webdavPassword
        )
      )
    } yield Webdav(sardine, conf.researchDrive, conf.webdavBase)
}

case class Webdav(sardine: Sardine,
                  rdConf: ResearchDriveConf,
                  val webdavBase: WebdavPath) {

  private val downloadFilesPipe: Pipe[IO, (WebdavPath, Path), BFile] = {
    msgStream =>
      for {
        paths <- msgStream
        (fromRoot, toRoot) = paths
        lp <- findAll(fromRoot).evalMap { from =>
          val toFile = BFile(toRoot) / from.userPath.get
          logger.info(
            s"Copying remote file ${from.toString} to local: ${toFile.toString}"
          ) >> Resource
            .fromAutoCloseable[IO, InputStream](IO {
              sardine.get(from.toURI.toString)
            })
            .use(copyStreamToLocalFile(_, toFile))
            .map(_ => toFile)
        }
      } yield lp
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

  def listAll(path: WebdavPath): IO[List[DavResource]] = IO {
    sardine
      .list(path.toURI.toString, rdConf.maxFolderDepth)
      .asScala
      .toList
  }

  def eTag(path: WebdavPath): IO[ETag] = {
    import cats.implicits._
    import nl.surf.dex.storage.owncloud.implicits._
    for {
      rr <- listAll(path)
      r <- IO.fromEither(
        rr.find(r => webdavBase.change(r) === path)
          .toRight(new FileNotFoundException(s"Webdav $path not found"))
      )
    } yield r.getSafeETag
  }

  private def findAll(webdavRoot: WebdavPath): fs2.Stream[IO, WebdavPath] =
    fs2.Stream
      .evals(listAll(webdavRoot))
      .collect {
        case r if !r.isDirectory => webdavRoot.change(r)
      }

  def downloadAllToHost(from: WebdavPath, to: Path)(
    implicit cs: ContextShift[IO]
  ): IO[List[BFile]] =
    fs2.Stream
      .emit[IO, (WebdavPath, Path)]((from, to))
      .through(downloadFilesPipe)
      .compile
      .toList

  /**
    * Downloads artifacts into their locations on the host.
    */
  def downloadToHost(
    map: Map[WebdavPath, Path]
  )(implicit F: Concurrent[IO]): IO[Unit] =
    fs2.Stream
      .emits[IO, (WebdavPath, Path)](map.view.toList)
      .balanceThrough(4)(downloadFilesPipe)
      .map(_ => ())
      .compile
      .drain

  private def copyStreamToLocalFile(from: InputStream, to: BFile): IO[Unit] =
    IO {
      to.toJava.getParentFile.mkdirs()
      Files.copy(from, to.path)
      ()
    }

}
