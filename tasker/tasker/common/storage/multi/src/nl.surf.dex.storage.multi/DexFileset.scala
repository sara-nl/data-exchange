package nl.surf.dex.storage.multi

import cats.effect.{ContextShift, IO, Resource}
import nl.surf.dex.storage.CloudStorage.{GoogleDrive, ResearchDrive}
import nl.surf.dex.storage.gdrive.GDriveFileset
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.owncloud.Webdav
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import nl.surf.dex.storage.{CloudStorage, FilesetOps}

object DexFileset {

  private def availableStorages(implicit
      cs: ContextShift[IO]
  ): Map[CloudStorage, Resource[IO, FilesetOps]] =
    Map(
      ResearchDrive -> (for {
        conf <- Resource.liftF(DexResearchDriveConf.loadIO)
        webdavR <- Webdav.makeWebdavClient.run(conf)
      } yield webdavR),
      GoogleDrive -> Resource.liftF(for {
        ccc <- DexGDriveConf.loadIO
        gDrive <- IO.pure(GDriveFileset.make(ccc))
      } yield gDrive)
    )

  def forStorage(
      storage: CloudStorage
  )(implicit cs: ContextShift[IO]): Resource[IO, FilesetOps] =
    for {
      storage <-
        availableStorages
          .getOrElse(
            storage,
            Resource.liftF[IO, FilesetOps](
              IO.raiseError(
                new RuntimeException(
                  s"Can not find fileset ops service for $storage"
                )
              )
            )
          )
    } yield storage

}
