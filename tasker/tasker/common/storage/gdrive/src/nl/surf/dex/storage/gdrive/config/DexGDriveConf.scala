package nl.surf.dex.storage.gdrive.config

import java.nio.file.Path

import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig

object DexGDriveConf extends DexConfig("storage.gdrive") {

  def loadIO(implicit cs: ContextShift[IO]): IO[DexGDriveConf] = {
    import pureconfig.generic.auto._
    Blocker[IO].use(configSrc.loadF[IO, DexGDriveConf])
  }

}

case class DexGDriveConf(
    applicationName: String,
    credentialsFile: Path,
    tokensDirectory: Path,
    receiverPort: Int
)
