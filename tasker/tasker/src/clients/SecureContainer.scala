package clients
import java.nio.file.{Path, Paths}

import cats.effect.{IO, _}
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

  def lastStatusIO(containerId: String): IO[Option[(String, Int)]] = SecureContainer
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

  def outputStream(containerId: String)(
    implicit F: ConcurrentEffect[IO],
    cs: ContextShift[IO]
  ): IO[String] = {
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
        allLogsCommand(containerId).exec(new LogContainerResultCallback {
          override def onNext(item: Frame): Unit = {
            val line = new String(item.getPayload)
            F.runAsync(q.enqueue1(Some(line)))(_ => IO.unit).unsafeRunSync
          }
        }).awaitCompletion()
      }
      _ <- q.enqueue1(None)
      lastOption <- q.dequeue.reduce[String]((l1: String, l2: String) => s"$l1\n$l2").compile.last
    } yield lastOption.getOrElse("")
  }

  def createContainer[F[_]: Sync: Logger](codeRoot: Path,
                                            dataRoot: Path,
                                            codeRelativePath: String,
                                            dataRelativePath: String): F[String] = Sync[F].delay {


      val logger = Logger[F]
      val hostCodePath = Paths.get(codeRoot.toString, codeRelativePath)

      val dockerScriptPath =
        if (hostCodePath.toFile.isDirectory)
          Paths.get(docker.containerCodePath, codeRelativePath, TaskerConfig.docker.indexFile)
        else
          Paths.get(docker.containerCodePath, codeRelativePath)

      val dockerDataPath = Paths.get(docker.containerDataPath, dataRelativePath)

      logger.info(s"Code path on host (docker): $hostCodePath ($dockerScriptPath)")
      logger.info(s"Data path on host (docker): $dataRoot ($dockerDataPath)")

      val createContainerCommand = client
        .createContainerCmd("python")
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
              )
            ).asJava
          )
        )
        .withCmd("python3", dockerScriptPath.toString, dockerDataPath.toString)
        .withAttachStdin(true)
        .withAttachStderr(true)

      createContainerCommand.exec().getId
    }

  def startContainer(containerId: String): IO[String] = IO {
    client.startContainerCmd(containerId).exec()
    containerId
  }

}
