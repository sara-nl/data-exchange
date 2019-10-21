package clients

import cats.effect.{IO, _}
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import config.TaskerConfig
import config.TaskerConfig.docker
import container.ContainerEnv

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

  def createContainer(containerEnv: ContainerEnv): IO[String] = {
    for {
      dockerScriptPath <- containerEnv.codeArtifact.executablePath
      command <- IO {
        client
          .createContainerCmd(TaskerConfig.docker.image)
          .withNetworkDisabled(true)
          .withHostConfig(
            new HostConfig().withBinds(
              List(
                containerEnv.codeArtifact.asBind(AccessMode.ro),
                containerEnv.dataArtifact.asBind(AccessMode.ro),
                containerEnv.outputArtifact.asBind(AccessMode.rw)
              ).asJava
            )
          )
          .withCmd(
            "/app/tracerun.sh",
            dockerScriptPath.toString,
            containerEnv.dataArtifact.containerPath.toString,
            s"${docker.containerOutPath}/stdout.txt",
            s"${docker.containerOutPath}/error.txt",
            s"${docker.containerOutPath}/trace.txt"
          )
          .withAttachStdin(true)
          .withAttachStderr(true)
      }
      result <- IO(command.exec())
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
