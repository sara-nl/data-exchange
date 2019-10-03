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
import io.circe.syntax._
import io.circe.parser.decode



class DockerFlow(consumer: Stream[IO, AmqpEnvelope[String]],
                 publisher: AmqpMessage[String] => IO[Unit]) {
  import io.circe.generic.auto._

  private val jsonEncoder = new Fs2JsonEncoder
  import jsonEncoder.jsonEncode

//  val jsonEncodePipe: Pipe[IO, AmqpMessage[Messages.StartContainer], AmqpMessage[String]] = _.map(jsonEncode[Messages.StartContainer])
  val jsonDecodePipe: Pipe[IO, AmqpEnvelope[String], Either[io.circe.Error, Messages.StartContainer]] = _.map(env => decode[Messages.StartContainer](env.payload))

  val flow: Stream[IO, Unit] =
    consumer.through(jsonDecodePipe).evalMap {
      case Right(startContainer) =>
        val doneMsg = Messages.Done(startContainer.taskId, "SUCCESS", "123")
        print("Replying with ", doneMsg.asJson.spaces2)
        publisher(AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties()))
      case Left(ex) =>
        println("Error: ", ex.getMessage)
        IO.unit
    }
}