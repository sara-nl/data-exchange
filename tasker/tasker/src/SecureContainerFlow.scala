import java.nio.file.Paths

import cats.effect._
import cats.implicits._
import clients.{SecureContainer, Webdav}
import dev.profunktor.fs2rabbit.json.Fs2JsonEncoder
import dev.profunktor.fs2rabbit.model._
import fs2._
import io.circe.syntax._
import io.circe.parser.decode

import scala.language.postfixOps

import io.chrisdavenport.log4cats.Logger
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger


class SecureContainerFlow(consumer: Stream[IO, AmqpEnvelope[String]],
                          publisher: AmqpMessage[String] => IO[Unit]) {
  import io.circe.generic.auto._

  private val jsonEncoder = new Fs2JsonEncoder
  import jsonEncoder.jsonEncode

//  val jsonEncodePipe: Pipe[IO, AmqpMessage[Messages.StartContainer], AmqpMessage[String]] = _.map(jsonEncode[Messages.StartContainer])
  val jsonDecodePipe: Pipe[IO, AmqpEnvelope[String], Either[io.circe.Error, Messages.StartContainer]] =
    _.map(env => decode[Messages.StartContainer](env.payload))

  val executeCodePipe: Pipe[IO, Messages.StartContainer, (String, Either[String, String])] = _.evalMap { msg =>
    Webdav.codeAndDataResources(msg.codePath, msg.dataPath).use {
      case (tempDir, codeIS, dataIS) =>
        val codeHome = Paths.get(tempDir.toString, "code")
        val dataHome = Paths.get(tempDir.toString, "data")

        val codeDownloadPath = Paths.get(codeHome.toString, msg.codePath)
        val dataDownloadPath = Paths.get(dataHome.toString, msg.dataPath)

        import scala.concurrent.ExecutionContext.Implicits.global
        implicit val ctx = IO.contextShift(global)

        val downloadIO = for {
          _ <- Webdav.copyStreamToLocalFile(codeIS, codeDownloadPath).start
          _ <- Webdav.copyStreamToLocalFile(dataIS, dataDownloadPath).start
        } yield ()

        val joinLines: (String, String) => String = { case (l1: String, l2: String) => s"$l1\n$l2" }

        for {
          _ <- downloadIO
          logger <- Slf4jLogger.create[IO]
          containerId <- SecureContainer.createContainer(codeHome, dataHome, msg.codePath, msg.dataPath)
          _ <- SecureContainer.startContainer(containerId)
          stateAndCode <- SecureContainer.statusStream(containerId).compile.last
          _ <- logger.info(stateAndCode.toString)
          outputOption <- SecureContainer.outputStream(containerId).reduce(joinLines).compile.last
        } yield {
          (msg.taskId, stateAndCode match {
            case Some((_, 0)) => outputOption.toRight(s"Could not get status and logs of container ${containerId}")
            case Some((_, nonZeroCode)) => Left(s"Docker container exited with $nonZeroCode")
            case None => Left(s"Could not get status of container. Should never happen.")
          })

        }
    }
  }

  val flow: Stream[IO, Unit] =
    consumer
      .through(jsonDecodePipe)
    .flatMap {
      case Right(value) => Stream.emit(value).through(executeCodePipe)
        .evalMap {
          case (taskId, Right(output)) =>
            val doneMsg = Messages.Done(taskId, "success", output)
//            print("Replying with ", doneMsg.asJson.spaces2)
            publisher(AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties()))
          case (taskId, Left(error)) =>
//            println("Error: ", error)
            val doneMsg = Messages.Done(taskId, "error", error)
            publisher(AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties()))
        }
      case Left(error) => Stream.eval(IO {
        error.printStackTrace()
      })
    }
}