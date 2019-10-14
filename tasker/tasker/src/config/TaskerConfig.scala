package config

import java.net.URI

import cats.effect.IO
import clients.webdav.WebdavPath
import com.github.dockerjava.core.DefaultDockerClientConfig
import dev.profunktor.fs2rabbit.config.declaration.{
  DeclarationExchangeConfig,
  DeclarationQueueConfig
}
import dev.profunktor.fs2rabbit.model.{
  ExchangeName,
  ExchangeType,
  QueueName,
  RoutingKey
}

object TaskerConfig {

  import cats.data.NonEmptyList
  import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}

  object concurrency {
    object implicits {
      import scala.concurrent.ExecutionContext.Implicits.global
      implicit val ctxShiftGlobal = IO.contextShift(global)
    }

  }

  object webdav {
    val username = "f_data_exchange"
    val password = "KCVNI-VBXWR-NLGMO-POQNO"
    val basePath = "/remote.php/nonshib-webdav/"
    val url = "https://researchdrive.surfsara.nl"
    val maxFolderDepth = 50
    val serverPath = WebdavPath(new URI(url), basePath)
  }

  object docker {
    val indexFile = "run.py"
    val containerCodePath = "/tmp/code"
    val containerDataPath = "/tmp/data"
    val defaultImage = "python"
    val clientConfig = DefaultDockerClientConfig
      .createDefaultConfigBuilder()
      .withDockerHost("unix:///var/run/docker.sock")
      .build();
  }

  object queues {
    object todo {
      private val name = "tasker_todo"
      val config = DeclarationQueueConfig.default(QueueName(name))
      val exchangeConfig =
        DeclarationExchangeConfig.default(
          ExchangeName(name),
          ExchangeType.Direct
        )
      val routingKey = RoutingKey(name)
    }
    object done {
      private val name = "tasker_done"
      val config = DeclarationQueueConfig.default(QueueName(name))
      val exchangeConfig =
        DeclarationExchangeConfig.default(
          ExchangeName(name),
          ExchangeType.Direct
        )
      val routingKey = RoutingKey(name)
    }
  }

  val rabbitConfig = Fs2RabbitConfig(
    virtualHost = "/",
    nodes = NonEmptyList.one(
      Fs2RabbitNodeConfig(
        host = sys.env.get("RABBITMQ_HOST").getOrElse("127.0.0.1"),
        port = 5672,
      ),
    ),
    username = sys.env.get("RABBITMQ_USERNAME"),
    password = sys.env.get("RABBITMQ_PASSWORD"),
    ssl = false,
    connectionTimeout = 3,
    requeueOnNack = false,
    internalQueueSize = Some(500),
    automaticRecovery = true
  ).copy(connectionTimeout = 5000)

}
