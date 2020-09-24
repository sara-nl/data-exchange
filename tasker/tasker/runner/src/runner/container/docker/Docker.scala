package runner.container.docker

import com.github.dockerjava.api.model.{AccessMode, Bind, HostConfig, Volume}
import runner.container.Artifact._
import runner.container.{Artifact, ContainerEnv}

import scala.jdk.CollectionConverters._

object Docker {

  object implicits {

    implicit class WithBindOps(val containerEnv: ContainerEnv) extends AnyVal {

      private def mkBind(location: Location, accessMode: AccessMode) =
        new Bind(
          location.localHome.toString,
          new Volume(location.containerHome.toString),
          accessMode
        )

      private def asBind(artifact: Artifact): Bind =
        artifact match {
          case Algorithm(location, _, _) =>
            mkBind(location, AccessMode.ro)
          case InputData(location) =>
            mkBind(location, AccessMode.ro)
          case OutputData(location) =>
            mkBind(location, AccessMode.rw)
        }

      def hostConfig: HostConfig =
        new HostConfig().withBinds(
          List(
            asBind(containerEnv.algorithm),
            asBind(containerEnv.input),
            asBind(containerEnv.output)
          ).asJava
        )

    }
  }

}
