package tasker.queue

import cats.data.Kleisli
import cats.effect.IO
import dev.profunktor.fs2rabbit.effects.{EnvelopeDecoder, MessageEncoder}
import dev.profunktor.fs2rabbit.model.{AmqpEnvelope, AmqpMessage}
import io.chrisdavenport.log4cats.Logger
import io.circe
import io.circe.parser.decode
import io.circe.syntax._
import io.circe.{Decoder, Encoder}

object AmqpCodecs {

  type DecodedMessage[P] = Either[circe.Error, P]

  /**
    * Decoder of incoming messages.
    */
  def decoder[T: Decoder]: EnvelopeDecoder[IO, DecodedMessage[T]] =
    AmqpEnvelope.stringDecoder[IO].map(decode[T])

  /**
    * Encoder for messages that have a Circe decoder
    */
  def encoder[T: Encoder]: MessageEncoder[IO, T] =
    Kleisli[IO, T, String](msg => IO.pure(msg.asJson.spaces2))
      .andThen(AmqpMessage.stringEncoder[IO])

  /**
    * Pipe that filters out successfully decoded messages and logs the bad ones via provided logger.
    */
  def filterAndLogErrors[P](
    logger: Logger[IO]
  ): fs2.Pipe[IO, DecodedMessage[P], P] =
    _.evalTap {
      case Left(error) =>
        logger.error(error)("Could not decode incoming message")
      case Right(_) =>
        IO.unit
    }.collect {
        case Right(payload) => payload
      }

}
