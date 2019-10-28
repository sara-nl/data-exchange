package runner

import cats.effect.{ExitCode, IO, IOApp, Resource}
import runner.config.TaskerConfig
import runner.config.TaskerConfig.queues
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.model.{AMQPChannel, AmqpMessage}
import Codecs._

object RunnerApp extends IOApp {

  def program(client: Fs2Rabbit[IO]): IO[Unit] = {
    val connChannel: Resource[IO, AMQPChannel] = client.createConnectionChannel
    connChannel.use { implicit channel =>
      for {
        _ <- client.declareQueue(queues.todo.config)
        _ <- client.declareExchange(queues.todo.exchangeConfig)
        _ <- client.bindQueue(
          queues.todo.config.queueName,
          queues.todo.exchangeConfig.exchangeName,
          queues.todo.routingKey
        )
        _ <- client.declareQueue(queues.done.config)
        _ <- client.declareExchange(queues.done.exchangeConfig)
        _ <- client.bindQueue(
          queues.done.config.queueName,
          queues.done.exchangeConfig.exchangeName,
          queues.done.routingKey
        )
        consumer <- client.createAutoAckConsumer[String](
          queues.todo.config.queueName
        )
        publisher <- client.createPublisher[AmqpMessage[String]](
          queues.done.exchangeConfig.exchangeName,
          queues.done.routingKey
        )
        _ <- new SecureContainerFlow(consumer, publisher).flow.compile.drain
      } yield ()
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    Resources.blockerResource.use { blocker =>
      for {
        client <- Fs2Rabbit[IO](TaskerConfig.rabbitConfig, blocker)
        _ <- program(client)
      } yield ExitCode.Success
    }
  }
}
