package nl.surf.dex.storage.config

import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig
import nl.surf.dex.storage.config.DexStorageConf.ResearchDriveConf
import nl.surf.dex.storage.owncloud.WebdavPath
import pureconfig.ConfigSource

object DexStorageConf extends DexConfig("storage") {

//  @deprecated("Use loadIO() if possible", "0.0")
// TODO: fix the deprecation
  def loadF: IO[DexStorageConf] = {
    import pureconfig.generic.auto._
    IO.fromEither(
      ConfigSource.default
        .at(namespace)
        .load[DexStorageConf]
        .left
        .map(ff => new RuntimeException(ff.prettyPrint(2)))
    )
  }

  def loadIO(implicit cs: ContextShift[IO]): IO[DexStorageConf] = {
    import pureconfig.generic.auto._
    Blocker[IO].use(configSrc.loadF[IO, DexStorageConf])
  }

  case class ResearchDriveConf(webdavUsername: String,
                               webdavPassword: String,
                               maxFolderDepth: Short)

}

//TODO: Assumption: there is only one webdav path
case class DexStorageConf(researchDrive: ResearchDriveConf,
                          webdavBase: WebdavPath)
