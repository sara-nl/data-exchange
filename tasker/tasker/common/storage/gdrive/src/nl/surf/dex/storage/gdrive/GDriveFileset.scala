package nl.surf.dex.storage.gdrive

import better.files.{File => BFile}
import cats.data.{Kleisli, NonEmptyList}
import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.{File => GFile}
import nl.surf.dex.storage.FilesetOps.errors
import nl.surf.dex.storage.FilesetOps.errors.CanNotListException
import nl.surf.dex.storage.Share.{Location, NePath}
import nl.surf.dex.storage.gdrive.GDriveFileset.{
  downloadGFile,
  listChildFilesOf
}
import nl.surf.dex.storage.gdrive.GDriveFilters.topLevel
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.{CloudStorage, Fileset, FilesetOps, Share}

import scala.jdk.CollectionConverters._

object GDriveFileset {

  def make(driveConf: DexGDriveConf): FilesetOps =
    new GDriveFileset(GDriveClient.makeClientK.run(driveConf))

  def isDirectory(file: GFile) =
    file.getMimeType === GDriveClient.FolderMimeType

  private[gdrive] def listShares: Kleisli[IO, Drive, List[GFile]] = Kleisli {
    drive =>
      IO(
        drive
          .files()
          .list() // see: https://developers.google.com/drive/api/v3/ref-search-terms
          .setQ(s"sharedWithMe=true")
          .setFields("*")
          .execute()
          .getFiles
          .asScala
          .toList
          .filter(topLevel)
      )
  }

  private[gdrive] def listChildFilesOf(
    parent: GFile
  ): Kleisli[IO, Drive, List[GFile]] =
    Kleisli { drive =>
      IO(
        drive
          .files()
          .list() // see: https://developers.google.com/drive/api/v3/ref-search-terms
          .setQ(s"'${parent.getId}' in parents")
          .setFields("*")
          .execute()
          .getFiles
          .asScala
          .toList
      )
    }

  private[gdrive] def getShare(path: Share.NePath): Kleisli[IO, Drive, GFile] =
    Kleisli { drive =>
      GDriveFileset.listShares.run(drive).flatMap { allShares =>
        IO.fromOption(allShares.find(_.getName === path.segments.head))(
          errors
            .notFoundException(Share.Location(CloudStorage.GoogleDrive, path))
        )
      }
    }

  private[gdrive] def downloadGFile(file: GFile, localParent: BFile)(
    implicit cs: ContextShift[IO]
  ): Kleisli[IO, Drive, BFile] = Kleisli { drive =>
    val destFile = localParent / file.getName
    file.getMimeType match {
      case GDriveClient.FolderMimeType =>
        (for {
          children <- listChildFilesOf(file)
          _ <- children.traverse(downloadGFile(_, destFile))
        } yield destFile).run(drive)
      case _ if file.getSize > 0 =>
        IO(localParent.createDirectories()) *>
          Resource
            .fromAutoCloseable(IO(destFile.newOutputStream))
            .use(
              os =>
                IO(drive.files().get(file.getId).executeMediaAndDownloadTo(os))
            ) *> IO(destFile)
      case _ =>
        // Just create an empty file
        IO(localParent.createDirectories()) *>
          IO(destFile.createFile())
    }
  }

  private def assumeDirectory(parent: GFile): IO[Unit] = parent match {
    case parent: GFile if GDriveFileset.isDirectory(parent) => IO.unit
    case parent: GFile =>
      IO.raiseError(
        CanNotListException(
          Location(CloudStorage.GoogleDrive, Share.NePath(parent.getName))
        )
      )
  }

}

class GDriveFileset(driveIO: IO[Drive]) extends FilesetOps {

  override def listShareFolder(
    sharePath: Share.NePath
  ): IO[List[Share.NePath]] =
    driveIO.flatMap {
      (for {
        share <- GDriveFileset.getShare(sharePath)
        _ <- Kleisli.liftF(GDriveFileset.assumeDirectory(share))
        children <- listChildFilesOf(share)
      } yield children.map(f => NePath(sharePath.segments :+ f.getName))).run
    }

  def getFilesetHash(userPath: Share.NePath): IO[Fileset.Hash] =
    driveIO.flatMap {
      (for {
        share <- GDriveFileset.getShare(userPath)
        children <- if (GDriveFileset.isDirectory(share)) {
          listChildFilesOf(share)
        } else {
          Kleisli.liftF(IO.pure(List(share)))
        }
        hashValue = hash.md5(
          children.sortBy(_.getId).map(_.getMd5Checksum).mkString(",")
        )
      } yield Fileset.Hash(hashValue)).run
    }

  override def copySharedFileset(path: Share.NePath, localParent: BFile)(
    implicit cs: ContextShift[IO]
  ): IO[BFile] = driveIO.flatMap {
    (path.segments match {
      case NonEmptyList(_, Nil) =>
        for {
          share <- GDriveFileset.getShare(path)
          downloaded <- downloadGFile(share, localParent)
        } yield downloaded
      case NonEmptyList(shareName, _) =>
        for {
          shareFileset <- GDriveFileset.getShare(path)
          _ <- Kleisli.liftF(GDriveFileset.assumeDirectory(shareFileset))
          children <- listChildFilesOf(shareFileset)
          child <- Kleisli.liftF(
            IO.fromOption(
              children
                .find(
                  f =>
                    shareName === path.segments.head &&
                      f.getName === path.segments.tail
                        .mkString("/")
                )
            )(
              FilesetOps.errors
                .notFoundException(Location(CloudStorage.GoogleDrive, path))
            )
          )
          downloaded <- downloadGFile(child, localParent / shareName)
        } yield downloaded
    }).run
  }

}
