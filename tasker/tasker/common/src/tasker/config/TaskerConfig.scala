package tasker.config

import java.net.URI
import java.util.concurrent.Executors

import cats.effect.{ContextShift, IO, Timer}
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
import io.netty.util.concurrent.DefaultThreadFactory
import tasker.webdav.WebdavPath

import scala.concurrent.ExecutionContext.fromExecutor
import scala.concurrent.duration._

object TaskerConfig {

  import cats.data.NonEmptyList
  import dev.profunktor.fs2rabbit.config.{Fs2RabbitConfig, Fs2RabbitNodeConfig}

  object watcher {
    val awakeInterval: FiniteDuration = 30.seconds
    val jdbcUrl = sys.env
      .getOrElse("DB_JDBC_URL", "jdbc:postgresql://localhost:5433/surfsara")
    val dbUser = sys.env.getOrElse("DB_USER", "surfsara")
    val dbPassword = sys.env.getOrElse("DB_PASSWORD", "")
  }

  object concurrency {

    def newCachedTPContextShift(label: String) = IO.contextShift(
      fromExecutor(
        Executors.newCachedThreadPool(new DefaultThreadFactory(label, true))
      )
    )

    def newTimer(label: String): Timer[IO] = IO.timer(
      fromExecutor(
        Executors.newFixedThreadPool(
          3,
          new DefaultThreadFactory("watcher-timer", true)
        )
      )
    )
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
    val image = "datex:latest"
    val indexFile = "run.py"
    val requirementsFile = "requirements.txt"
    val containerCodePath = "/tmp/code"
    val containerDataPath = "/tmp/data"
    val containerOutPath = "/tmp/out"
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
        host = sys.env.getOrElse("RABBITMQ_HOST", "127.0.0.1"),
        port = 5672,
      ),
    ),
    username = sys.env.get("RABBITMQ_USERNAME"),
    password = sys.env.get("RABBITMQ_PASSWORD"),
    ssl = false,
    connectionTimeout = 5000,
    requeueOnNack = false,
    internalQueueSize = Some(500),
    automaticRecovery = true
  )

}
