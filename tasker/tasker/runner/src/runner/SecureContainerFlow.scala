package runner

import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.messaging.Messages
import nl.surf.dex.storage.owncloud.{Webdav, WebdavPath}
import runner.RunnerConf.DockerConf
import runner.container.Artifact.Location
import runner.container.docker.DockerOps
import runner.container.{
  ContainerCommand,
  ContainerEnv,
  ContainerState,
  LogMessages
}
import runner.utils.FilesIO
import tasker.concurrency.ConcurrencyResources.implicits.ctxShiftGlobal
import Messages._
import nl.surf.dex.storage.ETag

class SecureContainerFlow(consumer: fs2.Stream[IO, StartContainer],
                          publisher: TaskProgress => IO[Unit],
                          webdavBase: WebdavPath,
                          dockerConf: DockerConf) {

  private val logger = Slf4jLogger.getLogger[IO]

  private def runAlgorithm(containerEnv: ContainerEnv,
                           taskId: String): IO[ContainerState] =
    for {
      _ <- publisher(TaskProgress.running(taskId, Step.InstallingDependencies))
      runAlgorithmCmd <- ContainerCommand.runWithStrace(containerEnv)
      result <- Resources
        .bakedImageWithDeps(containerEnv, dockerConf)
        .use {
          case Right(imageId) =>
            publisher(TaskProgress.running(taskId, Step.ExecutingAlgorithm)) *>
              DockerOps
                .startedContainer(containerEnv, runAlgorithmCmd, imageId)
                .use(containerId => {
                  for {
                    state <- DockerOps.terminalStateIO(containerId)
                    _ <- logger
                      .info(s"$containerId execution stopped with $state")
                  } yield state
                })
          case Left(containerState) => IO.pure(containerState)
        }
    } yield result

  private def verifyETag(path: WebdavPath, expectedETag: ETag): IO[Boolean] = {

    import nl.surf.dex.storage.owncloud.implicits._
    for {
      webdav <- Webdav.makeClient
      resources <- webdav.list(path)
      _ <- resources
        .map(r => logger.debug(s"${r.getPath} ---> ${r.getSafeETag}"))
        .sequence
      pathOption <- resources
        .find(resource => webdavBase.change(resource) === path)
        .toRight(new RuntimeException(s"Could not find resource $path"))
        .liftTo[IO]
    } yield pathOption.getSafeETag == expectedETag
  }

  private def readText(locationIO: IO[Location]): IO[String] =
    for {
      location <- locationIO
      localPath <- location.localPath
      contentOrError <- FilesIO
        .readFileContent(localPath)
        .attempt
    } yield contentOrError.toOption.getOrElse("")

  private def processMessage(msg: Messages.StartContainer): IO[TaskProgress] = {
    publisher(TaskProgress.running(msg.taskId, Step.DownloadingFiles)) *>
      Resources.containerEnv(msg, dockerConf, webdavBase).use { containerEnv =>
        for {
          _ <- logger.debug(s"Container environment: $containerEnv")
          _ <- publisher(
            TaskProgress.running(msg.taskId, Step.CreatingContainer)
          )
          endState <- runAlgorithm(containerEnv, msg.taskId)
          stdoutContent <- readText(containerEnv.stdout)
          stderrContent <- readText(containerEnv.stderr)
          straceContent <- readText(containerEnv.strace)
          _ <- logger
            .debug(LogMessages.largeOutputWithNewlines("STDOUT", stdoutContent))
          _ <- logger
            .debug(LogMessages.largeOutputWithNewlines("STDERR", stderrContent))
          _ <- logger
            .trace(LogMessages.largeOutputWithNewlines("STRACE", straceContent))
          algorithmOutput = AlgorithmOutput(
            stdoutContent,
            stderrContent,
            straceContent
          )
          _ <- publisher(TaskProgress.running(msg.taskId, Step.CleaningUp))
        } yield
          endState match {
            case ContainerState.Exited(0, _, _) =>
              Messages.TaskProgress
                .success(msg.taskId, algorithmOutput)
            case ContainerState.Exited(x, _, _) =>
              Messages.TaskProgress
                .error(
                  msg.taskId,
                  s"Container exited with a non-zero code $x.",
                  algorithmOutput,
                  Step.ExecutingAlgorithm
                )
            case ContainerState.Unknown =>
              Messages.TaskProgress.error(
                msg.taskId,
                "Container ended up in an unknown state. Please contact the development team.",
                algorithmOutput,
                Step.ExecutingAlgorithm
              )
          }
      }
  }

  val flow: fs2.Stream[IO, Unit] =
    consumer
      .evalTap(msg => logger.debug(s"Processing incoming message $msg"))
      .evalMap {
        case msg @ StartContainer(taskId, _, codePath, eTag) =>
          for {
            _ <- publisher(
              TaskProgress.running(taskId, Step.VerifyingAlgorithm)
            )
            eTagValidOrError <- verifyETag(
              webdavBase.change(codePath),
              ETag(eTag)
            ).attempt
            doneMsg <- eTagValidOrError match {
              case Right(true)  => processMessage(msg)
              case Right(false) => TaskProgress.rejectedEtag(taskId).pure[IO]
              case Left(ex)     => TaskProgress.rejected(taskId, ex).pure[IO]
            }
            _ <- logger.debug(
              s"The final state of $taskId is encoded as ${doneMsg.state}"
            )
            _ <- publisher(doneMsg)
          } yield ()
      }
}
