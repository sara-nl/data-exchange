package cacher.conf
import java.net.URI

import cacher.conf.CacherConf.{ClientConf, ServerConf}
import cats.effect.IO
import io.github.mkotsur.artc.ActiveReadThroughCache
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}

import scala.concurrent.duration._

object CacherConf {

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def loadF: IO[CacherConf] =
    ConfigSource.default.at("cacher").loadF[IO, CacherConf]

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
