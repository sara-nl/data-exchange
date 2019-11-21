package runner.clients

import cats.effect.{IO, _}
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import cats.implicits._
import runner.container.Ids.{ContainerId, ImageId}
import runner.container.{ContainerCommand, ContainerEnv, ContainerState}
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.jdk.CollectionConverters._

object DockerContainer {

  private val dockerClient = DockerClientBuilder.getInstance().build()

  def terminalStateIO(
    containerId: ContainerId
  )(implicit F: ConcurrentEffect[IO]): IO[ContainerState] =
    for {
      lastStatusOption <- lastStatusIO(containerId)
      output <- outputStream(containerId)
    } yield
      lastStatusOption match {
        case Some((dockerState, exitCode)) =>
          ContainerState.Exited(exitCode, dockerState, output)
        case _ => ContainerState.Unknown
      }

  /**
    * Returns IO of the last state and the exit code.
    */
  private def lastStatusIO(
    containerId: ContainerId
  ): IO[Option[(String, Int)]] =
    DockerContainer
      .statusStream(containerId)
      .compile
      .last

  private def statusStream(
    containerId: ContainerId
  ): fs2.Stream[IO, (String, Int)] = {
    fs2.Stream
      .eval(IO {
        dockerClient.inspectContainerCmd(containerId.value).exec().getState
      })
      .flatMap {
        case state if state.getRunning =>
          fs2.Stream((state.getStatus, state.getExitCode.toInt)) ++ statusStream(
            containerId
          )
        case state => fs2.Stream((state.getStatus, state.getExitCode.toInt))
      }
  }

  private def outputStream(
    containerId: ContainerId
  )(implicit F: ConcurrentEffect[IO]): IO[String] = {
    import fs2.concurrent._

    val allLogsCommand =
      dockerClient
        .logContainerCmd(containerId.value)
        .withFollowStream(false)
        .withTailAll()
        .withStdErr(true)
        .withStdOut(true)
        .withTimestamps(false)

    for {
      q <- Queue.noneTerminated[IO, String]
      _ <- IO.delay {
        allLogsCommand
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

  /**
    * Makes a snapshot of the container and returns the ID of an image
    */
  private def createImage(containerId: ContainerId): IO[ImageId] =
    IO(
      ImageId(
        dockerClient
          .commitCmd(containerId.value)
          .withTag("datex/tasker")
          .exec(),
        Some(containerId)
      )
    ).flatMap { imageId =>
      Slf4jLogger
        .getLogger[IO]
        .info(s"Created $imageId from $containerId") *> IO.pure(imageId)
    }

  /**
    * Removes the image with the given ID
    */
  private def removeImage(imageId: ImageId): IO[Unit] =
    IO(
      dockerClient
        .removeImageCmd(imageId.value)
        .exec()
    ) *> Slf4jLogger
      .getLogger[IO]
      .info(s"Removed $imageId")

  /**
    * Resource of a docker container.
    * Acquire: creates a new container and executes the command.
    * Release: removes the container.
    */
  def startedContainer(containerEnv: ContainerEnv,
                       cmd: ContainerCommand,
                       imageId: ImageId): Resource[IO, ContainerId] =
    Resource.make(for {
      containerId <- DockerContainer
        .createContainer(containerEnv, cmd, imageId)
      _ <- startContainer(containerId)
    } yield containerId)(removeContainer)

  /**
    * Resource of an image created from a container.
    * Acquire: creates an image of a container using `snapshot` operation.
    * Release: removes the image.
    */
  def imageFromContainer(containerId: ContainerId): Resource[IO, ImageId] =
    Resource.make(createImage(containerId))(removeImage)

  private def createContainer(containerEnv: ContainerEnv,
                              command: ContainerCommand,
                              imageId: ImageId): IO[ContainerId] = {
    for {
      dockerCommand <- IO {
        dockerClient
          .createContainerCmd(imageId.value)
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
      containerId <- IO(ContainerId(dockerCommand.exec().getId))
      _ <- Slf4jLogger.getLogger[IO].debug(s"Created $containerId")
    } yield containerId
  }

  private def startContainer(containerId: ContainerId): IO[ContainerId] =
    IO(dockerClient.startContainerCmd(containerId.value).exec()) *>
      Slf4jLogger
        .getLogger[IO]
        .info(s"Started $containerId") *>
      IO.pure(containerId)

  private def removeContainer(containerId: ContainerId): IO[Unit] =
    IO {
      dockerClient.removeContainerCmd(containerId.value).exec()
    } *> Slf4jLogger.getLogger[IO].info(s"Removed $containerId")

}
