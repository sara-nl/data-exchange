package watcher

import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig

import scala.concurrent.duration.FiniteDuration

object WatcherConf extends DexConfig("watcher") {

  def loadIO(implicit cs: ContextShift[IO]): IO[WatcherConf] = {
    import pureconfig.generic.auto._

    Blocker[IO].use(configSrc.loadF[IO, WatcherConf])
  }

}

case class WatcherConf(awakeInterval: FiniteDuration)
