package nl.surf.dex.storage.gdrive

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import nl.surf.dex.storage.gdrive.GDriveFileset._
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.{CloudStorage, Share}

object GDriveShares {
  import GDriveClient._

  def getShares: Kleisli[IO, DexGDriveConf, List[Share]] = Kleisli { conf =>
    for {
      client <- makeClientK.run(conf)
      topFiles <- GDriveFileset.listShares.run(client)
      shares <- topFiles.map { // Mapping over all top level entities
        case gFile if GDriveFileset.isDirectory(gFile) =>
          listChildFilesOf(gFile)
            .run(client)
            .map { children =>
              Share(
                CloudStorage.GoogleDrive,
                gFile.getName,
                isAlgorithm = children.exists(_.getName === "run.py"),
                isDirectory = true,
                gFile.getSharingUser.getEmailAddress,
                gFile.getWebViewLink
              )
            }
        case gFile =>
          Share(
            CloudStorage.GoogleDrive,
            gFile.getName,
            isAlgorithm = gFile.getName.endsWith(".py"),
            isDirectory = false,
            gFile.getSharingUser.getEmailAddress,
            gFile.getWebViewLink
          ).pure[IO]
      }.sequence
    } yield shares

  }

}
