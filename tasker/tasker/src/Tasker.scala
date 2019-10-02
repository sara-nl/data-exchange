import cats.MonadError
import cats.effect.{ExitCode, IO, IOApp, Resource, Sync}
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.model.AckResult.Ack
import dev.profunktor.fs2rabbit.model.{AMQPChannel, AckResult, AmqpEnvelope, ExchangeName, ExchangeType}
import fs2.Pipe
import cats.effect._
import cats.implicits._
import fs2._


object Tasker extends IOApp {

//  import io.circe.parser._
//  import io.circe.Json
//  implicit def jsonDecoder[F[_]](implicit F: MonadError[F, Throwable]): EnvelopeDecoder[F, Json] =
//    EnvelopeDecoder[F, String].flatMapF(s => F.fromEither(parse(s)))

  def program(client: Fs2Rabbit[IO]): IO[Unit] = {
    val connChannel: Resource[IO, AMQPChannel] = client.createConnectionChannel
    connChannel.use { implicit channel =>
      for {
        _ <- client.declareExchange(Config.exchangeName, ExchangeType.Topic)
        _ <- client.declareQueue(Config.queueConfig)
        _ <- client.bindQueue(Config.queueConfig.queueName, Config.exchangeName, Config.routingKey)
        consumer <- client.createAutoAckConsumer[String](Config.queueConfig.queueName)
        _ <- new AutoAckFlow[IO, String](consumer, logPipe).flow.compile.drain
      } yield ()
    }
  }

  def logPipe: Pipe[IO, AmqpEnvelope[String], AckResult] = _.evalMap { amqpMsg =>
    Sync[IO].delay(println(s"Consumed: $amqpMsg")).as(Ack(amqpMsg.deliveryTag))
  }

  override def run(args: List[String]): IO[ExitCode] = {
    Resources.blockerResource.use { blocker =>
      Fs2Rabbit[IO](Config.rabbitConfig, blocker).flatMap { client =>
        program(client) >> IO(ExitCode.Success)
      }
    }
  }
}
