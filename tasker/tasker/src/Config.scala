import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.model.{ExchangeName, QueueName, RoutingKey}

object Config {

  import cats.data.NonEmptyList
  import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}

  // See https://www.rabbitmq.com/tutorials/amqp-concepts.html#exchange-default
  val exchangeName = ExchangeName("tasker") // Empty string = default exchange

  val queueConfig = DeclarationQueueConfig.default(QueueName("tasker"))

  val routingKey = RoutingKey("tasker")

  val rabbitConfig = Fs2RabbitConfig(
    virtualHost = "/",
    nodes = NonEmptyList.one(
      Fs2RabbitNodeConfig(
        host = "127.0.0.1",
        port = 5672
      )
    ),
    username = Some("guest"),
    password = Some("guest"),
    ssl = false,
    connectionTimeout = 3,
    requeueOnNack = false,
    internalQueueSize = Some(500),
    automaticRecovery = true
  )

}