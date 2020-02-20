package tasker.config

import cats.effect.IO
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import tasker.config.CommonConf.{QueuesConf, RabbitmqConf, ResearchDriveConf}
import tasker.webdav.WebdavPath

import scala.concurrent.duration.FiniteDuration

object CommonConf {
  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def loadF: IO[CommonConf] =
    ConfigSource.default.at("common").loadF[IO, CommonConf]

  case class ResearchDriveConf(webdavUsername: String,
                               webdavPassword: String,
                               maxFolderDepth: Short)

  case class QueueConf(name: String, exchangeName: String, routingKey: String)
  case class QueuesConf(todo: QueueConf, done: QueueConf)

  case class RabbitmqConf(virtualHost: String,
                          host: String,
                          port: Int,
                          username: Option[String],
                          password: Option[String],
                          ssl: Boolean,
                          connectionTimeout: FiniteDuration)
}

case class CommonConf(researchDrive: ResearchDriveConf,
                      queues: QueuesConf,
                      rabbitmq: RabbitmqConf,
                      webdavBase: WebdavPath)
