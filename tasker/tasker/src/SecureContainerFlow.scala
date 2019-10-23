import Messages.ContainerOutput
import cats.effect._
import clients.DockerContainer.{removeContainer, startContainer}
import clients.webdav.{Webdav, WebdavPath}
import clients.DockerContainer
import container.ContainerCommand
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
          Slf4jLogger.create[IO].flatMap { logger =>
            Resources
              .containerEnvironmentResource(startContainerCmd)
              .use { containerEnv =>
                val artifactsToBeDownloaded = Map(
                  WebdavPath(startContainerCmd.codePath) -> containerEnv.codeArtifact,
                  WebdavPath(startContainerCmd.dataPath) -> containerEnv.dataArtifact
                )
                import config.TaskerConfig.concurrency.implicits.ctxShiftGlobal

                val containerOutputIO = for {
                  _ <- logger.info(s"Container environment: ${containerEnv}")
                  _ <- Webdav.downloadToHost(artifactsToBeDownloaded)
                  requirementsFileOption <- containerEnv.codeArtifact.requirementsFile
                  _ <- {
                    requirementsFileOption match {
                      case Some(requirements) =>
                        for {
                          _ <- logger
                            .info(s"Installing requirements from $requirements")
                          installReqCmd <- ContainerCommand
                            .installDeps(requirements)
                          containerId <- DockerContainer
                            .createContainer(containerEnv, installReqCmd)
                          _ <- logger.info(
                            s"Container with installed dependencies: ${containerId}"
                          )
                          _ <- startContainer(containerId)
                          stateAndCode <- DockerContainer
                            .lastStatusIO(containerId)
                          _ <- logger.info(
                            s"Container $containerId execution stopped with ${stateAndCode.toString}"
                          )
                          imageId <- DockerContainer.commit(containerId)
                          _ <- logger.info(
                            s"Image id with installed dependencies: $imageId"
                          )
                          scriptOutput <- DockerContainer
                            .outputStream(containerId)
                          _ <- logger.info(s"Container output: $scriptOutput")
                        } yield ()
                      case None =>
                        logger.info("There are no requirements to install")
                    }
                  }
                  runAlgorithmCmd <- ContainerCommand
                    .runWithStrace(containerEnv)
                  runAlgorithmCntId <- DockerContainer
                    .createContainer(containerEnv, runAlgorithmCmd)
                  _ <- logger.info(s"Created container $runAlgorithmCntId")
                  _ <- startContainer(runAlgorithmCntId)
                  stateAndCode <- DockerContainer
                    .lastStatusIO(runAlgorithmCntId)
                  _ <- logger.info(
                    s"Container $runAlgorithmCntId execution stopped with ${stateAndCode.toString}"
                  )
                  scriptOutput <- DockerContainer
                    .outputStream(runAlgorithmCntId)
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
                  _ <- removeContainer(runAlgorithmCntId)
                  _ <- logger.info(s"Removed container: $runAlgorithmCntId")
                } yield
                  (
                    stateAndCode,
                    (
                      s"$scriptOutput\n$stdoutContent\n$stderrContent",
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
