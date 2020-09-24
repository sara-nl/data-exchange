package runner.container

import cats.effect.IO
import runner.container.Artifact._

/**
  * Container environment
  */
case class ContainerEnv(algorithm: Algorithm, input: InputData, output: OutputData) {
  def stdout: IO[Location] =
    output.location
      .child("stdout.txt")

  def stderr: IO[Location] =
    output.location
      .child("stderr.txt")

  def strace: IO[Location] =
    output.location
      .child("strace.txt")
}
