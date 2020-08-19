package shares.conf

import shares.conf.SharesConf.{ClientConf, ServerConf}
import cats.effect.{ContextShift, IO}
import io.github.mkotsur.artc.ActiveReadThroughCache
import nl.surf.dex.config.DexConfig

import scala.concurrent.duration._

object SharesConf extends DexConfig("shares") {

  def loadIO(implicit cs: ContextShift[IO]): IO[SharesConf] = {
    import pureconfig.generic.auto._
    blocker.use(configSrc.loadF[IO, SharesConf])
  }

  case class ServerConf(idleTimeout: FiniteDuration,
                        responseHeaderTimeout: FiniteDuration)

  case class ClientConf(idleTimeout: FiniteDuration,
                        requestTimeout: FiniteDuration,
                        connectionTimeout: FiniteDuration,
                        responseHeaderTimeout: FiniteDuration)
}

case class SharesConf(server: ServerConf,
                      client: ClientConf,
                      update: ActiveReadThroughCache.Settings)