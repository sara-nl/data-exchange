package tasker.queue

import cats.effect.IO
import dev.profunktor.fs2rabbit.effects.EnvelopeDecoder
import dev.profunktor.fs2rabbit.model.AmqpEnvelope
import io.circe
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode

object Codecs {

  val startContainerDecoder
    : EnvelopeDecoder[IO, Either[circe.Error, Messages.StartContainer]] = for {
    v <- AmqpEnvelope.stringDecoder[IO]
  } yield decode[Messages.StartContainer](v)

}
