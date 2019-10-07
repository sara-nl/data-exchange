import cats.MonadError
import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.model.AckResult.Ack
import dev.profunktor.fs2rabbit.model.{AMQPChannel, AckResult, AmqpEnvelope, AmqpMessage, ExchangeName, ExchangeType}
import fs2.Pipe
import cats.implicits._
import Codecs._
import config.TaskerConfig
import config.TaskerConfig.queues


object Tasker extends IOApp {

//  import io.circe.parser._
//  import io.circe.Json
//  implicit def jsonDecoder[F[_]](implicit F: MonadError[F, Throwable]): EnvelopeDecoder[F, Json] =
//    EnvelopeDecoder[F, String].flatMapF(s => F.fromEither(parse(s)))

  def program(client: Fs2Rabbit[IO]): IO[Unit] = {
    val connChannel: Resource[IO, AMQPChannel] = client.createConnectionChannel
    connChannel.use { implicit channel =>
      for {
        _ <- client.declareQueue(queues.todo.config)
        _ <- client.declareExchange(queues.todo.exchangeConfig)
        _ <- client.bindQueue(queues.todo.config.queueName, queues.todo.exchangeConfig.exchangeName, queues.todo.routingKey)
        _ <- client.declareQueue(queues.done.config)
        _ <- client.declareExchange(queues.done.exchangeConfig)
        _ <- client.bindQueue(queues.done.config.queueName, queues.done.exchangeConfig.exchangeName, queues.done.routingKey)
        consumer <- client.createAutoAckConsumer[String](queues.todo.config.queueName)
        publisher <- client.createPublisher[AmqpMessage[String]](queues.done.exchangeConfig.exchangeName, queues.done.routingKey)
        _ <- new SecureContainerFlow(consumer, publisher).flow.compile.drain
      } yield ()
    }
  }

  override def run(args: List[String]): IO[ExitCode] = {
    Resources.blockerResource.use { blocker =>
      Fs2Rabbit[IO](TaskerConfig.rabbitConfig, blocker).flatMap { client =>
        program(client) >> IO(ExitCode.Success)
      }
    }
  }
}
