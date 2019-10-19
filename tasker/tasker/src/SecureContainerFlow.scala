import java.nio.file.Paths

import cats.effect._
import clients.SecureContainer.{removeContainer, startContainer}
import clients.webdav.{Webdav, WebdavPath}
import clients.SecureContainer
import dev.profunktor.fs2rabbit.json.Fs2JsonEncoder
import dev.profunktor.fs2rabbit.model._
import fs2._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.parser.decode
import io.circe.syntax._

import scala.language.postfixOps

class SecureContainerFlow(consumer: Stream[IO, AmqpEnvelope[String]],
                          publisher: AmqpMessage[String] => IO[Unit]) {
  import io.circe.generic.auto._

  val jsonDecodePipe
    : Pipe[IO, AmqpEnvelope[String], Either[io.circe.Error,
                                            Messages.StartContainer]] =
    _.map(env => decode[Messages.StartContainer](env.payload))

  val flow: Stream[IO, Unit] =
    consumer
      .through(jsonDecodePipe)
      .evalMap {
        case Right(startContainerCmd) =>
          Slf4jLogger.create[IO].flatMap { implicit logger =>
            Resources.tempDirResource.use { tempHome =>
              val codeHome = Paths.get(tempHome.toString, "code")
              val dataHome = Paths.get(tempHome.toString, "data")

              val filesDownloadedIO = Stream
                .emits(List(
                  (WebdavPath(startContainerCmd.codePath), codeHome),
                  (WebdavPath(startContainerCmd.dataPath), dataHome)
                ))
                .through(Webdav.downloadFilesPipe)
                .compile
                .drain

              val createContainerIO = SecureContainer.createContainer[IO](
                codeHome,
                dataHome,
                startContainerCmd.codePath,
                startContainerCmd.dataPath
              )

              import config.TaskerConfig.concurrency.implicits.ctxShiftGlobal

              val containerResultIO = for {
                _ <- logger.info(s"All files will be staged in ${tempHome.toString}")
                _ <- filesDownloadedIO
                containerId <- createContainerIO
                _ <- logger.info(s"Created container $containerId")
                _ <- startContainer(containerId)
                stateAndCode <- SecureContainer.lastStatusIO(containerId)
                _ <- logger.info(s"Container $containerId execution stopped with ${stateAndCode.toString}")
                output <- SecureContainer.outputStream(containerId)
                _ <- logger.info(s"Container output: $output")
                _ <- removeContainer(containerId)
                _ <- logger.info(s"Removed container: $containerId")
              } yield (stateAndCode, output)

              containerResultIO.flatMap {
                case (Some((_, 0)), output) =>
                  val doneMsg =
                    Messages.Done.success(startContainerCmd.taskId, output)
                  publisher(AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties()))
                case (Some((state, nonZeroCode)), output) =>
                  val doneMsg =
                    Messages.Done.error(
                      startContainerCmd.taskId,
                      s"Docker container exited in state $state with code $nonZeroCode. \n $output"
                    )
                  publisher(AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties()))
                case other =>
                  logger.error(s"Container state was $other. Should never happen.")
              }
            }
          }
        case Left(error) => for {
          logger <- Slf4jLogger.create[IO]
          _ <- logger.error(error)("Could not parse incoming message")
        } yield ()
      }
}
