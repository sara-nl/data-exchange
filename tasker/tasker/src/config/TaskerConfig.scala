package config

import com.github.dockerjava.core.DefaultDockerClientConfig
import dev.profunktor.fs2rabbit.config.declaration.{DeclarationExchangeConfig, DeclarationQueueConfig}
import dev.profunktor.fs2rabbit.model.{ExchangeName, ExchangeType, QueueName, RoutingKey}

object TaskerConfig {

  import cats.data.NonEmptyList
  import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}

  object webdav {
    val username = "f_data_exchange"
    val password = "KCVNI-VBXWR-NLGMO-POQNO"
    val url = "https://researchdrive.surfsara.nl/remote.php/nonshib-webdav/"
  }

  object docker {
    val containerCodePath = "/tmp/code"
    val containerDataPath = "/tmp/data"
    val defaultImage = "python"
    val clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder()
      .withDockerHost("unix:///var/run/docker.sock")
      .build();
  }

  object queues {
    object todo {
      private val name = "tasker_todo"
      val config = DeclarationQueueConfig.default(QueueName(name))
      val exchangeConfig = DeclarationExchangeConfig.default(ExchangeName(name), ExchangeType.Direct)
      val routingKey = RoutingKey(name)
    }
    object done {
      private val name = "tasker_done"
      val config = DeclarationQueueConfig.default(QueueName(name))
      val exchangeConfig = DeclarationExchangeConfig.default(ExchangeName(name), ExchangeType.Direct)
      val routingKey = RoutingKey(name)
    }
  }

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
  ).copy(connectionTimeout = 1000)

}