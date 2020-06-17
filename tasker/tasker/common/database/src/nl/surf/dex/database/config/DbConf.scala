package nl.surf.dex.database.config

import cats.effect.{ContextShift, IO}
import nl.surf.dex.config.DexConfig

object DbConf extends DexConfig("database") {
  def loadIO(implicit cs: ContextShift[IO]): IO[DbConf] = {
    import pureconfig.generic.auto._
    blocker.use(configSrc.loadF[IO, DbConf])
  }
}

case class DbConf(jdbcUrl: String, username: String, password: String)
