package runner

import cats.effect._
import clients.DockerContainer
import tasker.config.TaskerConfig.concurrency.implicits.ctxShiftGlobal
import runner.container.{
  ContainerCommand,
  ContainerEnv,
  ContainerState,
  LogMessages
}
import cats.implicits._
import dev.profunktor.fs2rabbit.model._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import tasker.queue.Messages.{AlgorithmOutput, Done, ETag, StartContainer}
import runner.utils.FilesIO
import tasker.queue.{AmqpCodecs, Messages}
import tasker.webdav.{Webdav, WebdavPath}

class SecureContainerFlow(consumer: fs2.Stream[IO, AmqpEnvelope[
                            AmqpCodecs.DecodedMessage[StartContainer]
                          ]],
                          publisher: Done => IO[Unit]) {

  private val logger = Slf4jLogger.getLogger[IO]

  private def runAlgorithm(
    containerEnv: ContainerEnv,
    runAlgorithmCmd: ContainerCommand
  ): IO[ContainerState] =
    for {
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
                    .info(s"$containerId execution stopped with $state")
                } yield state
              })
          case Left(containerState) => IO.pure(containerState)
        }
    } yield result

  private def verifyETag(path: WebdavPath, expectedETag: ETag): IO[Boolean] = {
    import Webdav.implicits._
    for {
      resources <- Webdav.list(path)
      _ <- IO(
        resources.foreach(r => println(s"${r.getPath} ---> ${r.getEtag}"))
      )
      pathOption <- IO.pure(
        resources.find(resource => WebdavPath(resource) == path)
      )
    } yield pathOption.exists(_.getSafeETag == expectedETag)
  }

  private def processMessage(msg: Messages.StartContainer): IO[Done] = {
    Resources.containerEnv(msg).use { containerEnv =>
      val toBeDownloaded = Map(
        WebdavPath(msg.codePath) -> containerEnv.codeArtifact.hostHome,
        WebdavPath(msg.dataPath) -> containerEnv.dataArtifact.hostHome
      )

      for {
        _ <- logger.debug(s"Container environment: $containerEnv")
        _ <- Webdav.downloadToHost(toBeDownloaded)
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
        _ <- logger
          .info(LogMessages.largeOutputWithNewlines("STDOUT", stdoutContent))
        _ <- logger
          .info(LogMessages.largeOutputWithNewlines("STDERR", stderrContent))
        _ <- logger
          .trace(LogMessages.largeOutputWithNewlines("STRACE", straceContent))
        algorithmOutput = AlgorithmOutput(
          stdoutContent,
          stderrContent,
          straceContent
        )
      } yield
        endState match {
          case ContainerState.Exited(0, _, output) =>
            val taskUserOutput = s"$output\n$stdoutContent\n$stderrContent"
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

  val flow: fs2.Stream[IO, Unit] =
    consumer
      .map(_.payload)
      .through(AmqpCodecs.filterAndLogErrors(logger))
      .evalTap(msg => logger.info(s"Processing incoming message $msg"))
      .evalMap {
        case StartContainer(taskId, _, _, None) =>
          logger.error(
            s"Task $taskId is rejected because the incoming message doesn't contain the algorithm hash."
          )
        case msg @ StartContainer(taskId, _, codePath, Some(eTag)) =>
          for {
            eTagValid <- verifyETag(WebdavPath(codePath), eTag)
            doneMsg <- if (eTagValid)
              processMessage(msg)
            else
              Done
                .rejected(
                  taskId,
                  "The algorithm mush have changed since approval. ETag doesn't match."
                )
                .pure[IO]
            _ <- logger.info(s"The state of Done message is ${doneMsg.state}")
            _ <- publisher(doneMsg)
          } yield ()
      }
}
