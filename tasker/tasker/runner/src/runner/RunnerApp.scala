package runner

import cats.effect.{ExitCode, IO, IOApp}
import dev.profunktor.fs2rabbit.model.AmqpMessage
import tasker.config.TaskerConfig.queues
import tasker.queue.Codecs._
import tasker.queue.QueueResources

object RunnerApp extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    QueueResources.rabbitClientResource.use { rabbit =>
      rabbit.createConnectionChannel.use { implicit channel =>
        for {
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
          consumer <- rabbit.createAutoAckConsumer[String](
            queues.todo.config.queueName
          )
          publisher <- rabbit.createPublisher[AmqpMessage[String]](
            queues.done.exchangeConfig.exchangeName,
            queues.done.routingKey
          )
          _ <- new SecureContainerFlow(consumer, publisher).flow.compile.drain
        } yield ExitCode.Success
      }
    }
  }
}
