package runner

import cats.data.Kleisli
import cats.effect._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import nl.surf.dex.messaging.Messages
import nl.surf.dex.messaging.Messages._
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
import cats.implicits._
import nl.surf.dex.storage.{CloudStorage, FilesetOps}
import runner.SecureContainerFlow.Deps

object SecureContainerFlow {

  case class Deps(
      consumer: fs2.Stream[IO, StartContainer],
      publisher: TaskProgress => IO[Unit],
      storageFactory: CloudStorage => Resource[IO, FilesetOps],
      dockerConf: DockerConf
  )
}

class SecureContainerFlow(deps: Deps) {

  private val logger = Slf4jLogger.getLogger[IO]

  private def runAlgorithm(
      containerEnv: ContainerEnv,
      taskId: String
  ): IO[ContainerState] =
    for {
      _ <- deps.publisher(
        TaskProgress.running(taskId, Step.InstallingDependencies)
      )
      runAlgorithmCmd <- ContainerCommand.runWithStrace(containerEnv)
      result <-
        Resources
          .bakedImageWithDeps(containerEnv, deps.dockerConf)
          .use {
            case Right(imageId) =>
              deps.publisher(
                TaskProgress.running(taskId, Step.ExecutingAlgorithm)
              ) *>
                DockerOps
                  .startedContainer(containerEnv, runAlgorithmCmd, imageId)
                  .use(containerId => {
                    for {
                      state <- DockerOps.terminalStateIO(containerId)
                      _ <-
                        logger
                          .info(s"$containerId execution stopped with $state")
                    } yield state
                  })
            case Left(containerState) => IO.pure(containerState)
          }
    } yield result

  private def readText(locationIO: IO[Location]): IO[String] =
    for {
      location <- locationIO
      localPath <- location.localPath
      contentOrError <-
        FilesIO
          .readFileContent(localPath)
          .attempt
    } yield contentOrError.toOption.getOrElse("")

  private def processMessage(
      msg: Messages.StartContainer
  ): Kleisli[IO, CloudStorage => Resource[IO, FilesetOps], TaskProgress] =
    Kleisli { filesetOpsFactory =>
      deps.publisher(TaskProgress.running(msg.taskId, Step.DownloadingFiles)) *>
        Resources.containerEnv(msg, deps.dockerConf, filesetOpsFactory).use {
          containerEnv =>
            for {
              _ <- logger.debug(s"Container environment: $containerEnv")
              _ <-
                deps
                  .publisher(
                    TaskProgress.running(msg.taskId, Step.CreatingContainer)
                  )
              endState <- runAlgorithm(containerEnv, msg.taskId)
              stdoutContent <- readText(containerEnv.stdout)
              stderrContent <- readText(containerEnv.stderr)
              straceContent <- readText(containerEnv.strace)
              _ <-
                logger
                  .debug(
                    LogMessages.largeOutputWithNewlines("STDOUT", stdoutContent)
                  )
              _ <-
                logger
                  .debug(
                    LogMessages.largeOutputWithNewlines("STDERR", stderrContent)
                  )
              _ <-
                logger
                  .trace(
                    LogMessages.largeOutputWithNewlines("STRACE", straceContent)
                  )
              algorithmOutput = AlgorithmOutput(
                stdoutContent,
                stderrContent,
                straceContent
              )
              _ <-
                deps
                  .publisher(TaskProgress.running(msg.taskId, Step.CleaningUp))
            } yield endState match {
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
    deps.consumer
      .evalTap(msg => logger.debug(s"Processing incoming message $msg"))
      .evalMap {
        case msg @ StartContainer(taskId, _, code, hashStrOption) =>
          deps.storageFactory(code.storage).use { filesetOps =>
            for {
              _ <- deps.publisher(
                TaskProgress.running(taskId, Step.VerifyingAlgorithm)
              )
              eTagValidOrError <- (hashStrOption match {
                  case Some(hashStr) =>
                    filesetOps
                      .getFilesetHash(code.path)
                      .map(_.value === hashStr)
                  case None => IO.pure(true)
                }).attempt
              doneMsg <- eTagValidOrError match {
                case Right(true) => processMessage(msg).run(deps.storageFactory)
                case Right(false) =>
                  TaskProgress.rejectedDueToHash(taskId).pure[IO]
                case Left(ex) => TaskProgress.rejected(taskId, ex).pure[IO]
              }
              _ <- logger.debug(
                s"The final state of $taskId is encoded as ${doneMsg.state}"
              )
              _ <- deps.publisher(doneMsg)
            } yield ()
          }

      }
}
