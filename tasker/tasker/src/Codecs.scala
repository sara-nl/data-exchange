import cats.data.Kleisli
import cats.effect.IO
import dev.profunktor.fs2rabbit.model.AmqpMessage
import java.nio.charset.StandardCharsets.UTF_8
import cats.implicits._

object Codecs {

  implicit val stringMessageEncoder =
    Kleisli[IO, AmqpMessage[String], AmqpMessage[Array[Byte]]](
      s => s.copy(payload = s.payload.getBytes(UTF_8)).pure[IO]
    )

}
