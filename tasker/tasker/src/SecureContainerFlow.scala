import java.io.FileInputStream

import Messages.ContainerOutput
import cats.effect._
import clients.SecureContainer.{removeContainer, startContainer}
import clients.webdav.{Webdav, WebdavPath}
import clients.SecureContainer
import dev.profunktor.fs2rabbit.model._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import utils.FilesIO

import scala.language.postfixOps

class SecureContainerFlow(consumer: fs2.Stream[IO, AmqpEnvelope[String]],
                          publisher: AmqpMessage[String] => IO[Unit]) {
  import io.circe.generic.auto._

  val flow: fs2.Stream[IO, Unit] =
    consumer
      .through(Codecs.messageDecodePipe)
      .evalMap {
        case Right(startContainerCmd) =>
          Slf4jLogger.create[IO].flatMap { implicit logger =>
            Resources
              .containerEnvironmentResource(startContainerCmd)
              .use { containerEnv =>
                val filesDownloadedIO = fs2.Stream
                  .emits(
                    List(
                      (
                        WebdavPath(startContainerCmd.codePath),
                        containerEnv.codeArtifact.hostHome
                      ),
                      (
                        WebdavPath(startContainerCmd.dataPath),
                        containerEnv.dataArtifact.hostHome
                      )
                    )
                  )
                  .through(Webdav.downloadFilesPipe)
                  .compile
                  .drain

                import config.TaskerConfig.concurrency.implicits.ctxShiftGlobal

                val containerOutputIO = for {
                  _ <- logger.info(s"Container environment: ${containerEnv}")
                  _ <- filesDownloadedIO
                  containerId <- SecureContainer.createContainer(containerEnv)
                  _ <- logger.info(s"Created container $containerId")
                  _ <- startContainer(containerId)
                  stateAndCode <- SecureContainer.lastStatusIO(containerId)
                  _ <- logger.info(
                    s"Container $containerId execution stopped with ${stateAndCode.toString}"
                  )
                  scriptOutput <- SecureContainer.outputStream(containerId)
                  stdoutContent <- FilesIO.readFileContent(
                    containerEnv.outputArtifact.hostStdoutFilePath
                  )
                  stderrContent <- FilesIO.readFileContent(
                    containerEnv.outputArtifact.hostStderrFilePath
                  )
                  straceContent <- FilesIO.readFileContent(
                    containerEnv.outputArtifact.hostStraceFilePath
                  )
                  _ <- logger.info(
                    s"\n ----- Start of STDOUT -----\n $stdoutContent \n ----- End of STDOUT -----"
                  )
                  _ <- logger.info(
                    s"\n ----- Start of STDERR -----\n $stderrContent \n ----- End of STDERR -----"
                  )
                  _ <- removeContainer(containerId)
                  _ <- logger.info(s"Removed container: $containerId")
                } yield
                  (
                    stateAndCode,
                    (
                      scriptOutput,
                      ContainerOutput(
                        stdoutContent,
                        stderrContent,
                        straceContent
                      )
                    )
                  )

                containerOutputIO.flatMap {
                  case (Some((_, 0)), (output, containerOutput)) =>
                    val doneMsg =
                      Messages.Done.success(
                        startContainerCmd.taskId,
                        output,
                        containerOutput
                      )
                    publisher(
                      AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties())
                    )
                  case (
                      Some((state, nonZeroCode)),
                      (output, containerOutput)
                      ) =>
                    val doneMsg =
                      Messages.Done.error(
                        startContainerCmd.taskId,
                        s"Docker container exited in state $state with code $nonZeroCode. \n $output",
                        containerOutput
                      )
                    publisher(
                      AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties())
                    )
                  case other =>
                    logger
                      .error(
                        s"Container state was $other. Should never happen."
                      )
                }
              }
          }
        case Left(error) =>
          for {
            logger <- Slf4jLogger.create[IO]
            _ <- logger.error(error)("Could not parse incoming message")
          } yield ()
      }
}
