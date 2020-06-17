package runner

import cats.effect.{Blocker, ContextShift, IO}
import nl.surf.dex.config.DexConfig
import runner.RunnerConf.DockerConf

object RunnerConf extends DexConfig("runner") {

  def loadIO(implicit cs: ContextShift[IO]): IO[RunnerConf] = {
    import pureconfig.generic.auto._
    Blocker[IO].use(configSrc.loadF[IO, RunnerConf])
  }

  case class DockerConf(image: String,
                        indexFile: String,
                        requirementsFile: String,
                        containerCodePath: String,
                        containerDataPath: String,
                        containerOutPath: String)

}

case class RunnerConf(docker: DockerConf)
