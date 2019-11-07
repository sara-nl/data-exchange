package runner

import cats.effect.{ExitCode, IO, IOApp}
import dev.profunktor.fs2rabbit.model.AmqpMessage
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe
import io.circe.Decoder
import tasker.config.TaskerConfig.queues
import tasker.queue.{Codecs, Messages, QueueResources}

object RunnerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    QueueResources.rabbitClientResource.use { rabbit =>
      rabbit.createConnectionChannel.use { implicit channel =>
        for {
          _ <- logger.info("Runner started")
          _ <- rabbit.declareQueue(queues.todo.config)
          _ <- rabbit.declareExchange(queues.todo.exchangeConfig)
          _ <- rabbit.bindQueue(
            queues.todo.config.queueName,
            queues.todo.exchangeConfig.exchangeName,
            queues.todo.routingKey
          )
          _ <- rabbit.declareQueue(queues.done.config)
          _ <- rabbit.declareExchange(queues.done.exchangeConfig)
          _ <- rabbit.bindQueue(
            queues.done.config.queueName,
            queues.done.exchangeConfig.exchangeName,
            queues.done.routingKey
          )
          consumer <- rabbit
            .createAutoAckConsumer[Either[circe.Error,
                                          Messages.StartContainer]](
              queues.todo.config.queueName
            )(channel, Codecs.startContainerDecoder)
          publisher <- rabbit.createPublisher[String](
            queues.done.exchangeConfig.exchangeName,
            queues.done.routingKey
          )(channel, AmqpMessage.stringEncoder[IO])
          _ <- new SecureContainerFlow(consumer, publisher).flow.compile.drain
        } yield ExitCode.Success
      }
    }
}
