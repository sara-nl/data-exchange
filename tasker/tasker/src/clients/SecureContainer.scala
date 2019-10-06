package clients
import java.nio.file.Path

import cats.effect.{IO, Timer}
import cats.effect._

import scala.concurrent.duration._
import com.github.dockerjava.api.model.{AccessMode, Bind, Frame, HostConfig, Volume}
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import config.TaskerConfig

import scala.concurrent.ExecutionContext
import scala.jdk.CollectionConverters._

import cats.effect._
import cats.implicits._
import cats.syntax._

object SecureContainer {
  val dockerCodePath = "/tmp/code"
  val dockerDataPath = "/tmp/data"

  private val client = DockerClientBuilder.getInstance().build()


  def startContainer(containerId: String): IO[String] = IO {

    client.startContainerCmd(containerId).exec()

    print(s"Started container with ${containerId}")
    containerId
  } >> cats.effect.IO.timer(ExecutionContext.global).sleep(120.seconds) >> IO {
    val cmd = client.logContainerCmd(containerId)
    cmd.withFollowStream(true)
    cmd.withStdErr(true)
    cmd.withStdOut(true)
    cmd.withTimestamps(true)
    val callback = cmd.withTailAll().exec(new LogContainerResultCallback)
    containerId
  }

//
//  def waitForContainerStop(containerId: String): IO[String] = IO {
//    client.cont
//  }



  def createContainer(codePath: Path, dataPath: Path, codeRelativePath: String, dataRelativePath: String): IO[String] = IO {
    val binds = List(
      new Bind(codePath.toString, new Volume(dockerCodePath), AccessMode.ro),
      new Bind(dataPath.toString, new Volume(dockerDataPath), AccessMode.ro)
    )

    val createContainerResponse = client.createContainerCmd(TaskerConfig.docker.defaultImage)
      .withNetworkDisabled(true)
      .withHostConfig(
        new HostConfig().withBinds(binds.asJava)
      )
      .withCmd("python3", s"${dockerCodePath}/${codeRelativePath}")
      .withAttachStdin(true)
      .withAttachStderr(true)
      .exec()

    println(s"Data path: $dockerDataPath/$dataRelativePath")

    createContainerResponse.getId
  }
}
