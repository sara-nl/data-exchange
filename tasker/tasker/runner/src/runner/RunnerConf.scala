package runner

import cats.effect.IO
import pureconfig.generic.ProductHint
import pureconfig.generic.auto._
import pureconfig.module.catseffect.syntax._
import pureconfig.{CamelCase, ConfigFieldMapping, ConfigSource}
import runner.RunnerConf.DockerConf

object RunnerConf {
  implicit def hint[T]: ProductHint[T] =
    ProductHint[T](ConfigFieldMapping(CamelCase, CamelCase))

  def loadF: IO[RunnerConf] =
    ConfigSource.default.at("runner").loadF[IO, RunnerConf]

  case class DockerConf(image: String,
                        indexFile: String,
                        requirementsFile: String,
                        containerCodePath: String,
                        containerDataPath: String,
                        containerOutPath: String)

}

case class RunnerConf(docker: DockerConf)
