package nl.surf.dex.messaging

import cats.data.NonEmptyList
import dev.profunktor.fs2rabbit.config.declaration.{
  DeclarationExchangeConfig,
  DeclarationQueueConfig
}
import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}
import dev.profunktor.fs2rabbit.model.{
  ExchangeName,
  ExchangeType,
  QueueName,
  RoutingKey
}

/**
  * This package object contains conversions from the application config
  * into library-specific configs using implicit classes. Some default parameters
  * are provided here as well.
  */
package object config {

  implicit class RabbitmqServerConfigConversion(conf: BrokerConf) {
    val fs2RabbitConfig: Fs2RabbitConfig = Fs2RabbitConfig(
      virtualHost = conf.virtualHost,
      nodes = NonEmptyList.one(Fs2RabbitNodeConfig(conf.host, port = conf.port)),
      username = conf.username,
      password = conf.password,
      ssl = conf.ssl,
      connectionTimeout = conf.connectionTimeout.toMillis.toInt,
      requeueOnNack = false,
      internalQueueSize = Some(500),
      automaticRecovery = true
    )
  }

  implicit class QueueConfigConversion(config: QueueConf) {
    private val qName = QueueName(config.name)

    private val xName = ExchangeName(config.exchangeName)

    val queueConfig: DeclarationQueueConfig =
      DeclarationQueueConfig.default(qName)

    val exchangeConfig: DeclarationExchangeConfig =
      DeclarationExchangeConfig.default(xName, ExchangeType.Direct)

    private val rKey: RoutingKey = RoutingKey(config.routingKey)
    val routingKey: RoutingKey = rKey

    def asTuple: (QueueName, ExchangeName, RoutingKey) =
      (qName, xName, rKey)
  }

}
