package cacher.conf
import java.net.URI

import cacher.conf.CacherConf.{ClientConf, ServerConf}
import cats.effect.{ContextShift, IO}
import io.github.mkotsur.artc.ActiveReadThroughCache
import nl.surf.dex.config.DexConfig

import scala.concurrent.duration._

object CacherConf extends DexConfig("cacher") {

  def loadIO(implicit cs: ContextShift[IO]): IO[CacherConf] = {
    import pureconfig.generic.auto._
    blocker.use(configSrc.loadF[IO, CacherConf])
  }

  case class ServerConf(idleTimeout: FiniteDuration,
                        responseHeaderTimeout: FiniteDuration)

  case class ClientConf(idleTimeout: FiniteDuration,
                        requestTimeout: FiniteDuration,
                        connectionTimeout: FiniteDuration,
                        responseHeaderTimeout: FiniteDuration)
}

case class CacherConf(server: ServerConf,
                      client: ClientConf,
                      update: ActiveReadThroughCache.Settings,
                      sharesSource: URI)
