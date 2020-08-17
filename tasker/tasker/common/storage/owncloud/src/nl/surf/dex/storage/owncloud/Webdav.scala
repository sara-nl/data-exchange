package nl.surf.dex.storage.owncloud

import java.io.{FileNotFoundException, InputStream}
import java.nio.file.{Files, Path}

import better.files.{File => BFile}
import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import com.github.sardine.{DavResource, Sardine, SardineFactory}
import fs2.Pipe
import nl.surf.dex.storage.FilesetOps.errors.CanNotListException
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import nl.surf.dex.storage.{CloudStorage, Fileset, FilesetOps, Share}

import scala.jdk.CollectionConverters._

object Webdav {

  def makeSardineR(conf: DexResearchDriveConf): Resource[IO, Sardine] =
    Resource.make(
      IO(SardineFactory.begin(conf.webdavUsername, conf.webdavPassword))
    )(sardine => IO(sardine.shutdown()))

  def makeSardine: Kleisli[Resource[IO, *], DexResearchDriveConf, Sardine] =
    Kleisli(makeSardineR)

  def makeWebdavClient
    : Kleisli[Resource[IO, *], DexResearchDriveConf, FilesetOps] =
    makeSardine.flatMap { sardine =>
      Kleisli { conf =>
        Webdav(sardine, conf)
          .pure[Resource[IO, *]]
      }
    }

}

case class Webdav(sardine: Sardine, rdConf: DexResearchDriveConf)
    extends FilesetOps {

  /**
    * Lists path contents (by default -- single level).
    */
  private[owncloud] def list(path: WebdavPath,
                             depth: Int = 1): IO[List[DavResource]] = IO {
    sardine
      .list(path.toURI.toString, depth)
      .asScala
      .toList
  }

  override def listShareFolder(
    sharePath: Share.NePath
  ): IO[List[Share.NePath]] = {
    val shareWebDavPath = rdConf.webdavBase.change(sharePath)
    for {
      childResources <- list(shareWebDavPath)
      _ <- {
        val shareWebdavPath = shareWebDavPath
        childResources.find(
          r => rdConf.webdavBase.change(r).equals(shareWebdavPath)
        ) match {
          case Some(r) if r.isDirectory => IO.pure(childResources)
          case _ =>
            IO.raiseError(
              CanNotListException(
                Share.Location(CloudStorage.ResearchDrive, sharePath)
              )
            )
        }
      }
      paths <- childResources
        .map(rdConf.webdavBase.change)
        .filter { _ != shareWebDavPath }
        .map(wdp => Share.NePath.parseIO(wdp.userPath))
        .sequence
    } yield paths

  }

  override def getFilesetHash(sharePath: Share.NePath): IO[Fileset.Hash] = {
    val path = rdConf.webdavBase.change(sharePath)
    import cats.implicits._
    import nl.surf.dex.storage.owncloud.implicits._
    for {
      rr <- list(path)
      r <- IO.fromEither(
        rr.find(r => rdConf.webdavBase.change(r) === path)
          .toRight(new FileNotFoundException(s"Webdav $path not found"))
      )
    } yield r.getSafeHash
  }

  private def findAll(webdavRoot: WebdavPath): fs2.Stream[IO, WebdavPath] =
    fs2.Stream
      .evals(list(webdavRoot, rdConf.maxFolderDepth))
      .collect {
        case r if !r.isDirectory => webdavRoot.change(r)
      }

  override def copySharedFileset(sharePath: Share.NePath, localParent: BFile)(
    implicit cs: ContextShift[IO]
  ): IO[BFile] = {
    val remoteTargetWebdavPath = rdConf.webdavBase.change(sharePath)
    fs2.Stream
      .emit((remoteTargetWebdavPath, localParent.path))
      .through(downloadFilesPipe)
      .compile
      .drain
      .map(_ => localParent / sharePath.segments.mkString_("/"))
  }

  private val downloadFilesPipe: Pipe[IO, (WebdavPath, Path), BFile] = {
    msgStream =>
      for {
        paths <- msgStream
        (fromRoot, toRoot) = paths
        lp <- findAll(fromRoot).evalMap { from =>
          val toFile = BFile(toRoot) / from.userPath.get
          Resource
            .fromAutoCloseable[IO, InputStream](IO {
              sardine.get(from.toURI.toString)
            })
            .use(copyStreamToLocalFile(_, toFile))
            .map(_ => toFile)
        }
      } yield lp
  }

  private def copyStreamToLocalFile(from: InputStream, to: BFile): IO[Unit] =
    IO {
      to.toJava.getParentFile.mkdirs()
      Files.copy(from, to.path)
      ()
    }

}
