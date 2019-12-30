package runner

import cats.effect.{ExitCode, IO, IOApp}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.config.TaskerConfig.queues
import tasker.queue.Messages.{StartContainer, TaskProgress}
import tasker.queue.{AmqpCodecs, QueueResources}

object RunnerApp extends IOApp {

  private val logger = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] =
    QueueResources.rabbitClientResource.use { rabbit =>
      rabbit.createConnectionChannel.use { implicit channel =>
        import tasker.queue.Messages.implicits._
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
            .createAutoAckConsumer(queues.todo.config.queueName)(
              channel,
              AmqpCodecs.decoder[StartContainer]
            )
          publisher <- rabbit.createPublisher(
            queues.done.exchangeConfig.exchangeName,
            queues.done.routingKey
          )(channel, AmqpCodecs.encoder[TaskProgress])
          _ <- new SecureContainerFlow(consumer, publisher).flow.compile.drain
        } yield ExitCode.Success
      }
    }
}
