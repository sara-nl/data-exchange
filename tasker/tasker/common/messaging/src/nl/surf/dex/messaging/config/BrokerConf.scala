package nl.surf.dex.messaging.config

import scala.concurrent.duration.FiniteDuration

case class BrokerConf(virtualHost: String,
                      host: String,
                      port: Int,
                      username: Option[String],
                      password: Option[String],
                      ssl: Boolean,
                      connectionTimeout: FiniteDuration)
