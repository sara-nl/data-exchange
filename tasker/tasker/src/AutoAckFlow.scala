import java.nio.charset.StandardCharsets.UTF_8

import cats.data.Kleisli
import cats.effect._
import cats.implicits._
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.interpreter.Fs2Rabbit
import dev.profunktor.fs2rabbit.json.Fs2JsonEncoder
import dev.profunktor.fs2rabbit.model.AckResult.Ack
import dev.profunktor.fs2rabbit.model.AmqpFieldValue.{LongVal, StringVal}
import dev.profunktor.fs2rabbit.model._
import fs2._

class AutoAckFlow[F[_]: Concurrent, A](
                                        consumer: Stream[F, AmqpEnvelope[A]],
                                        logger: Pipe[F, AmqpEnvelope[A], AckResult]
                                      ) {
  import io.circe.generic.auto._

  private val jsonEncoder = new Fs2JsonEncoder
  import jsonEncoder.jsonEncode

  val jsonPipe: Pipe[Pure, AmqpMessage[Messages.StartContainer], AmqpMessage[String]] = _.map(jsonEncode[Messages.StartContainer])

  val flow: Stream[F, Unit] =
    Stream(
      consumer.through(logger).evalMap(ack => Sync[F].delay(println(ack)))
    ).parJoin(3)

}