import cats.data.Kleisli
import cats.effect.IO
import dev.profunktor.fs2rabbit.model.{AmqpEnvelope, AmqpMessage}
import java.nio.charset.StandardCharsets.UTF_8

import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser.decode

object Codecs {

  implicit val messageEncoder =
    Kleisli[IO, AmqpMessage[String], AmqpMessage[Array[Byte]]](
      s => s.copy(payload = s.payload.getBytes(UTF_8)).pure[IO]
    )

  val messageDecodePipe
    : fs2.Pipe[IO, AmqpEnvelope[String], Either[io.circe.Error,
                                                Messages.StartContainer]] =
    _.map(env => decode[Messages.StartContainer](env.payload))

}
