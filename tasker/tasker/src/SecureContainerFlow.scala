import Messages.{AlgorithmOutput, Done}
import cats.effect._
import clients.webdav.{Webdav, WebdavPath}
import clients.DockerContainer
import container.{ContainerCommand, ContainerEnv, ContainerState, LogMessages}
import dev.profunktor.fs2rabbit.model._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import io.circe.syntax._
import utils.FilesIO
import config.TaskerConfig.concurrency.implicits.ctxShiftGlobal

import scala.language.postfixOps

class SecureContainerFlow(consumer: fs2.Stream[IO, AmqpEnvelope[String]],
                          publisher: AmqpMessage[String] => IO[Unit]) {
  import io.circe.generic.auto._

  private def runAlgorithm(
    containerEnv: ContainerEnv,
    runAlgorithmCmd: ContainerCommand
  ): IO[ContainerState] =
    for {
      logger <- Slf4jLogger.create[IO]
      requirementsFileOption <- containerEnv.codeArtifact.requirementsFile
      result <- Resources
        .bakedImageWithDeps(containerEnv, requirementsFileOption)
        .use {
          case Right(imageId) =>
            DockerContainer
              .startedContainer(containerEnv, runAlgorithmCmd, imageId)
              .use(containerId => {
                for {
                  state <- DockerContainer.terminalStateIO(containerId)
                  _ <- logger
                    .info(s"$containerId execution stopped with ${state}")
                } yield state
              })
          case Left(containerState) => IO.pure(containerState)
        }
    } yield result

  private def processMessage(msg: Messages.StartContainer): IO[Done] =
    for {
      logger <- Slf4jLogger.create[IO]
      done <- {
        Resources.containerEnv(msg).use { containerEnv =>
          val artifactsToBeDownloaded = Map(
            WebdavPath(msg.codePath) -> containerEnv.codeArtifact,
            WebdavPath(msg.dataPath) -> containerEnv.dataArtifact
          )

          for {
            _ <- logger.info(s"Container environment: $containerEnv")
            _ <- Webdav.downloadToHost(artifactsToBeDownloaded)
            runAlgorithmCmd <- ContainerCommand.runWithStrace(containerEnv)
            endState <- runAlgorithm(containerEnv, runAlgorithmCmd)
            stdoutContent <- FilesIO
              .readFileContent(containerEnv.outputArtifact.hostStdoutFilePath)
              .attempt
              .map(_.toOption.getOrElse(""))
            stderrContent <- FilesIO
              .readFileContent(containerEnv.outputArtifact.hostStderrFilePath)
              .attempt
              .map(_.toOption.getOrElse(""))
            straceContent <- FilesIO
              .readFileContent(containerEnv.outputArtifact.hostStraceFilePath)
              .attempt
              .map(_.toOption.getOrElse(""))
            _ <- logger.info(
              LogMessages.largeOutputWithNewlines("STDOUT", stdoutContent)
            )
            _ <- logger.info(
              LogMessages.largeOutputWithNewlines("STDERR", stderrContent)
            )
            _ <- logger.trace(
              LogMessages.largeOutputWithNewlines("STRACE", straceContent)
            )
            algorithmOutput = AlgorithmOutput(
              stdoutContent,
              stderrContent,
              straceContent
            )
          } yield
            endState match {
              case ContainerState.Exited(0, _, output) =>
                val taskUserOutput =
                  s"Container exited successfully\n$output\n$stdoutContent\n$stderrContent"
                Messages.Done
                  .success(msg.taskId, taskUserOutput, algorithmOutput)
              case ContainerState.Exited(x, _, output) =>
                val taskUserOutput =
                  s"Container exited with an error. Status code: $x\n$output\n$stdoutContent\n$stderrContent"
                Messages.Done
                  .error(msg.taskId, taskUserOutput, algorithmOutput)
              case ContainerState.Unknown =>
                Messages.Done.error(
                  msg.taskId,
                  "Container ended up in an unknown state. Please contact the development team.",
                  algorithmOutput
                )
            }
        }
      }
    } yield done

  val flow: fs2.Stream[IO, Unit] =
    consumer
      .through(Codecs.messageDecodePipe)
      .evalMap {
        case Right(startContainerCmd) =>
          for {
            doneMsg <- processMessage(startContainerCmd)
            logger <- Slf4jLogger.create[IO]
            _ <- logger.info(s"The DONE message is $doneMsg")
            _ <- publisher(
              AmqpMessage(doneMsg.asJson.spaces2, AmqpProperties())
            )
          } yield ()
        case Left(error) =>
          for {
            logger <- Slf4jLogger.create[IO]
            _ <- logger.error(error)("Could not parse incoming message")
          } yield ()
      }
}
