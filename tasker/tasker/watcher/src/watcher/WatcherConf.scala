package watcher

import cats.effect.IO
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import watcher.WatcherConf.DbConf

import scala.concurrent.duration.FiniteDuration

object WatcherConf {

  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def loadF: IO[WatcherConf] =
    ConfigSource.default.at("watcher").loadF[IO, WatcherConf]

  case class DbConf(jdbcUrl: String, username: String, password: String)

}

case class WatcherConf(db: DbConf, awakeInterval: FiniteDuration)
