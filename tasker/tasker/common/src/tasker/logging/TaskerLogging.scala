package tasker.logging

import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import cats.effect._
import cats.implicits._
import io.chrisdavenport.log4cats.Logger
import cats.effect.{IO, _}
import com.github.dockerjava.api.model._
import com.github.dockerjava.core.DockerClientBuilder
import com.github.dockerjava.core.command.LogContainerResultCallback
import IO.ioEffect
import cats.implicits._
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger

import scala.jdk.CollectionConverters._
import scala.language.postfixOps

object TaskerLogging {
  implicit def logger[F[_]: Sync] =
    Slf4jLogger.getLogger[IO]
}
