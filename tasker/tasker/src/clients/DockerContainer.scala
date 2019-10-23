package clients

import cats.effect.{IO, _}
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import config.TaskerConfig
import container.{ContainerCommand, ContainerEnv}

import scala.jdk.CollectionConverters._
import scala.language.postfixOps
import scala.util.Try

object DockerContainer {

  private val dockerClient = DockerClientBuilder.getInstance().build()

  def lastStatusIO(containerId: String): IO[Option[(String, Int)]] =
    DockerContainer
      .statusStream(containerId)
      .compile
      .last

  def statusStream(containerId: String): fs2.Stream[IO, (String, Int)] = {
    fs2.Stream
      .eval(IO {
        dockerClient.inspectContainerCmd(containerId).exec().getState
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
      dockerClient
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

  def commit(containerId: String) =
    IO(
      dockerClient
        .commitCmd(containerId)
        .withTag("datex/tasker")
        .exec()
    )

  def createContainer(containerEnv: ContainerEnv,
                      command: ContainerCommand): IO[String] = {
    for {
      dockerCommand <- IO {
        dockerClient
          .createContainerCmd(TaskerConfig.docker.image)
          .withNetworkDisabled(command.secureContainer)
          .withHostConfig(
            new HostConfig().withBinds(
              List(
                containerEnv.codeArtifact.asBind(AccessMode.ro),
                containerEnv.dataArtifact.asBind(AccessMode.ro),
                containerEnv.outputArtifact.asBind(AccessMode.rw)
              ).asJava
            )
          )
          .withCmd(command.toArgs.asJava)
          .withAttachStdin(true)
          .withAttachStderr(true)
      }
      result <- IO(dockerCommand.exec())
    } yield result.getId
  }

  def startContainer(containerId: String): IO[String] = IO {
    dockerClient.startContainerCmd(containerId).exec()
    containerId
  }

  def removeContainer(containerId: String): IO[String] = IO {
    dockerClient.removeContainerCmd(containerId).exec()
    containerId
  }

}
