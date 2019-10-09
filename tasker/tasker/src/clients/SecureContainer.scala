package clients
import java.nio.file.Path

import cats.effect.{IO, _}
import cats.implicits._
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import config.TaskerConfig.docker

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object SecureContainer {

  private val client = DockerClientBuilder.getInstance().build()

  def statusStream(containerId: String): fs2.Stream[IO, (String, Int)] = {
    fs2.Stream
      .eval(IO {
        client.inspectContainerCmd(containerId).exec().getState
      })
      .flatMap {
        case state if state.getRunning =>
          fs2.Stream((state.getStatus, state.getExitCode.toInt)) ++ statusStream(containerId)
        case state => fs2.Stream((state.getStatus, state.getExitCode.toInt))
      }
  }

  def outputStream(
      containerId: String
  )(implicit F: ConcurrentEffect[IO], cs: ContextShift[IO]): fs2.Stream[IO, String] = {
    import fs2._
    import fs2.concurrent._

    def allLogsCommand(containerId: String) =
      client
        .logContainerCmd(containerId)
        .withFollowStream(true)
        .withTailAll()
        .withStdErr(true)
        .withStdOut(true)
        .withTimestamps(false)

    for {
      q <- fs2.Stream.eval(Queue.noneTerminated[IO, String])
      _ <- Stream.eval {
        IO.delay {
          allLogsCommand(containerId).exec(new LogContainerResultCallback {
            override def onNext(item: Frame): Unit = {
              val line = new String(item.getPayload)
              F.runAsync(q.enqueue1(Some(line)))(_ => IO.unit).unsafeRunSync
            }
          })
        }
      }
      _ <- Stream.eval {
        import scala.concurrent.duration._
        implicit val timer = IO.timer(ExecutionContext.global)

        // TODO: get rid of sleep
        timer.sleep(1 second) >> q.enqueue1(None)
      }
      line <- q.dequeue
    } yield line
  }

  def createContainer(
      codePath: Path,
      dataPath: Path,
      codeRelativePath: String,
      dataRelativePath: String
  ): IO[String] = IO {
    val dockerCodePath = s"${docker.containerCodePath}/${codeRelativePath}"
    val dockerDataPath = s"${docker.containerDataPath}/${dataRelativePath}"

    println(s"Code path on host (docker): $codePath ($dockerCodePath)")
    println(s"Code path on host (docker): $dataPath ($dockerDataPath)")

    val createContainerCommand = client
      .createContainerCmd("python")
      .withNetworkDisabled(true)
      .withHostConfig(
        new HostConfig().withBinds(
          List(
            new Bind(codePath.toString, new Volume(docker.containerCodePath), AccessMode.ro),
            new Bind(dataPath.toString, new Volume(docker.containerDataPath), AccessMode.ro)
          ).asJava
        )
      )
      .withCmd("python3", dockerCodePath, dockerDataPath)
      .withAttachStdin(true)
      .withAttachStderr(true)

    createContainerCommand.exec().getId
  }

  def startContainer(containerId: String): IO[String] =
    IO {
      client.startContainerCmd(containerId).exec()
      println(s"Started container ${containerId}")
      containerId
    } >> IO.pure(containerId)

  def statusesAndLogs(contId: String)(implicit F: ConcurrentEffect[IO], sc: ContextShift[IO]) = {
    val statusStream = SecureContainer
      .statusStream(contId)
      .map(v => s"Container status: $v")
    val logsStream                            = SecureContainer.outputStream(contId)
    val joinLines: (String, String) => String = { case (l1: String, l2: String) => s"$l1\n$l2" }
    statusStream.mergeHaltBoth(logsStream).reduce(joinLines)
  }
}
