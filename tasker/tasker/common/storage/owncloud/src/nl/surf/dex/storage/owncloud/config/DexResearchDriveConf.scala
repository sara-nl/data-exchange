package nl.surf.dex.storage.owncloud.config

import io.lemonlabs.uri.Url
import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig
import nl.surf.dex.storage.owncloud.WebdavPath
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf.FileBrowserConf
import pureconfig.generic.auto._
import nl.surf.dex.config.codecs._
import pureconfig.generic.auto._
object DexResearchDriveConf extends DexConfig("storage.research-drive") {

  def loadIO(implicit cs: ContextShift[IO]): IO[DexResearchDriveConf] = {
    Blocker[IO].use(configSrc.loadF[IO, DexResearchDriveConf])
  }

  case class FileBrowserConf(baseUrl: Url, queryParamName: String)

  object FileBrowserConf {
    def resolve(conf: FileBrowserConf, value: String): Url =
      conf.baseUrl.mapQuery {
        case (queryParamName, _) =>
          (queryParamName, value)
      }
  }

}

case class DexResearchDriveConf(
    webdavUsername: String,
    webdavPassword: String,
    sharesSource: Url,
    fileBrowser: FileBrowserConf,
    maxFolderDepth: Short,
    webdavBase: WebdavPath
)
