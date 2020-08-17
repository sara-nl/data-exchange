package runner

import java.nio.file.{Path, Paths}

import cats.effect.{ConcurrentEffect, ContextShift, IO, Resource}
import nl.surf.dex.messaging.Messages
import nl.surf.dex.storage.{CloudStorage, FilesetOps, Share}
import runner.RunnerConf.DockerConf
import runner.container.Artifact.Location
import runner.container.docker.Ids.ImageId
import runner.container.docker.{DockerOps, Ids}
import runner.container.{
  Artifact,
  ContainerCommand,
  ContainerEnv,
  ContainerState
}
import better.files.{File => BFile}
import cats.implicits._
import nl.surf.dex.storage.local.LocalFS

object Resources {

  /**
    * Resource of a container environment: temporary directories, where all files can be downloaded.
    * Acquire: temp dir created.
    * Release: temp dir recursively deleted.
    */
  def containerEnv(startContainerCmd: Messages.StartContainer,
                   dockerConf: DockerConf,
                   filesetOpsFactory: CloudStorage => Resource[IO, FilesetOps],
  )(implicit cs: ContextShift[IO]): Resource[IO, ContainerEnv] =
    LocalFS.tempDir.evalMap { tempHome =>
      val algorithmLocation = Location(
        Paths.get(tempHome.toString, "code"),
        Path.of(dockerConf.containerCodePath),
        startContainerCmd.codeLocation.path.segments.mkString_("/")
      )

      val inputLocation = Location(
        Paths.get(tempHome.toString, "data"),
        Path.of(dockerConf.containerDataPath),
        startContainerCmd.dataLocation.path.segments.mkString_("/")
      )

      val outputLocation =
        Location(
          Paths.get(tempHome.toString, "out"),
          Path.of(dockerConf.containerOutPath),
          "."
        )

      val newDirs = List(algorithmLocation, inputLocation, outputLocation)

      for {
        _ <- newDirs.map(l => IO(l.localHome.toFile.mkdirs())).sequence
        _ <- List(
          (algorithmLocation, startContainerCmd.codeLocation.storage),
          (inputLocation, startContainerCmd.dataLocation.storage)
        ).parTraverse {
          case (loc, storage) =>
            filesetOpsFactory(storage).use { ops =>
              Share.NePath.parseIO(loc.userPath).flatMap { nePath =>
                ops.copySharedFileset(nePath, BFile(loc.localHome))
              }
            }
        }
        algorithm <- Artifact.algorithm(algorithmLocation)
        input <- Artifact.data(inputLocation)
        output <- Artifact.output(outputLocation)
      } yield ContainerEnv(algorithm, input, output)

    }

  /**
    * Resource of an image, which can be used for creating a container for running the algorithm.
    * Acquire: If necessary - container is created, dependencies installed, image created.
    * Release: If necessary - container and image removed.
    */
  def bakedImageWithDeps(containerEnv: ContainerEnv, dockerConf: DockerConf)(
    implicit F: ConcurrentEffect[IO]
  ): Resource[IO, Either[ContainerState, ImageId]] =
    containerEnv.algorithm.requirements match {
      case Some(requirementsLocation) =>
        for {
          reqContainerPath <- Resource.liftF(requirementsLocation.containerPath)
          containerId <- DockerOps
            .startedContainer(
              containerEnv,
              ContainerCommand.installDeps(reqContainerPath),
              Ids.ImageId(dockerConf.image)
            )
          containerState <- Resource.liftF(
            DockerOps.terminalStateIO(containerId)
          )
          result <- containerState match {
            case ContainerState.Exited(0, _, _) =>
              DockerOps
                .imageFromContainer(containerId)
                .map(Right(_).withLeft[ContainerState])
            case otherState =>
              Resource.pure[IO, Either[ContainerState, ImageId]](
                Left(otherState).withRight[ImageId]
              )
          }
        } yield result
      case None =>
        Resource.pure[IO, Either[ContainerState, ImageId]](
          Right(Ids.ImageId(dockerConf.image))
        )
    }

}
