package nl.surf.dex.storage.owncloud

import cats.data.Kleisli
import cats.effect.{IO, Resource}
import cats.implicits._
import com.github.sardine.{Sardine, SardineFactory}
import nl.surf.dex.storage.config.DexStorageConf

object WebdavResources {

  def makeSardine: Kleisli[Resource[IO, *], DexStorageConf, Sardine] =
    Kleisli { conf =>
      Resource.make(
        IO(
          SardineFactory.begin(
            conf.researchDrive.webdavUsername,
            conf.researchDrive.webdavPassword
          )
        )
      )(sardine => IO(sardine.shutdown()))
    }

  def makeWebdavClient: Kleisli[Resource[IO, *], DexStorageConf, Webdav] =
    makeSardine.flatMap { sardine =>
      Kleisli { conf =>
        Webdav(sardine, conf.researchDrive, conf.webdavBase)
          .pure[Resource[IO, *]]
      }
    }

}
