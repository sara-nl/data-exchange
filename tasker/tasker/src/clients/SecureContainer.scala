package clients
import java.nio.file.{Path, Paths}

import cats.effect.{IO, _}
import cats.implicits._
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import config.TaskerConfig
import config.TaskerConfig.docker
import io.chrisdavenport.log4cats.Logger

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object SecureContainer {

  private val client = DockerClientBuilder.getInstance().build()

  def lastStatusIO(containerId: String): IO[Option[(String, Int)]] =
    SecureContainer
      .statusStream(containerId)
      .compile
      .last

  def statusStream(containerId: String): fs2.Stream[IO, (String, Int)] = {
    fs2.Stream
      .eval(IO {
        client.inspectContainerCmd(containerId).exec().getState
      })
      .flatMap {
        case state if state.getRunning =>
          fs2.Stream((state.getStatus, state.getExitCode.toInt)) ++ statusStream(
            containerId
          )
        case state => fs2.Stream((state.getStatus, state.getExitCode.toInt))
      }
  }

  def outputStream(containerId: String)(implicit F: ConcurrentEffect[IO],
                                        cs: ContextShift[IO]): IO[String] = {
    import fs2.concurrent._

    def allLogsCommand(containerId: String) =
      client
        .logContainerCmd(containerId)
        .withFollowStream(false)
        .withTailAll()
        .withStdErr(true)
        .withStdOut(true)
        .withTimestamps(false)

    for {
      q <- Queue.noneTerminated[IO, String]
      _ <- IO.delay {
        allLogsCommand(containerId)
          .exec(new LogContainerResultCallback {
            override def onNext(item: Frame): Unit = {
              val line = new String(item.getPayload)
              F.runAsync(q.enqueue1(Some(line)))(_ => IO.unit).unsafeRunSync
            }
          })
          .awaitCompletion()
      }
      _ <- q.enqueue1(None)
      lastOption <- q.dequeue
        .reduce[String]((l1: String, l2: String) => s"$l1\n$l2")
        .compile
        .last
    } yield lastOption.getOrElse("")
  }

  def createContainer[F[_]: Sync: Logger](
    codeRoot: Path,
    dataRoot: Path,
    codeRelativePath: String,
    dataRelativePath: String
  ): F[String] = {
    val logger = Logger[F]
    for {
      hostCodePath <- Sync[F].pure(
        Paths.get(codeRoot.toString, codeRelativePath)
      )
      dockerScriptPath <- Sync[F].delay {
        if (hostCodePath.toFile.isDirectory)
          Paths.get(
            docker.containerCodePath,
            codeRelativePath,
            TaskerConfig.docker.indexFile
          )
        else
          Paths.get(docker.containerCodePath, codeRelativePath)
      }
      dockerDataPath <- Sync[F].pure(
        Paths.get(docker.containerDataPath, dataRelativePath)
      )
      command <- Sync[F].delay {
        client
          .createContainerCmd(TaskerConfig.docker.image)
          .withNetworkDisabled(true)
          .withHostConfig(
            new HostConfig().withBinds(
              List(
                new Bind(
                  codeRoot.toString,
                  new Volume(docker.containerCodePath),
                  AccessMode.ro
                ),
                new Bind(
                  dataRoot.toString,
                  new Volume(docker.containerDataPath),
                  AccessMode.ro
                ),
                new Bind(
                  "/tmp/tasker-out", // TODO: Make configurable
                  new Volume(docker.outputPath),
                  AccessMode.rw
                )
              ).asJava
            )
          )
          .withCmd(
            "/app/tracerun.sh",
            dockerScriptPath.toString,
            dockerDataPath.toString,
            s"${docker.outputPath}/out.txt",
            s"${docker.outputPath}/error.txt",
            s"${docker.outputPath}/trace.txt"
          )
          .withAttachStdin(true)
          .withAttachStderr(true)
      }
      _ <- logger.info(
        s"Code path on host (docker): $hostCodePath ($dockerScriptPath)"
      )
      _ <- logger.info(
        s"Data path on host (docker): $dataRoot ($dockerDataPath)"
      )
      result <- Sync[F].delay(command.exec())
    } yield result.getId
  }

  def startContainer(containerId: String): IO[String] = IO {
    client.startContainerCmd(containerId).exec()
    containerId
  }

  def removeContainer(containerId: String): IO[String] = IO {
    client.removeContainerCmd(containerId).exec()
    containerId
  }

}
