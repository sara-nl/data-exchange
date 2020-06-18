package nl.surf.dex.figurer.program
import better.files.{File => BFile}
import cats.data.Kleisli
import cats.effect.{ContextShift, IO, Resource}
import cats.implicits._
import nl.surf.dex.figurer.FigurerApp
import nl.surf.dex.storage.local.LocalFS

object PythonProgram {

  def apply(rootDir: BFile, file: BFile): IO[PythonProgram] = {
    if (file.name.endsWith(".py"))
      PythonProgram.apply(rootDir, Set(file)).pure[IO]
    else
      IO.raiseError(
        new IllegalArgumentException(s"$file is not a Python program")
      )
  }

  def downloadedInTempR(algorithmPath: String)(
    implicit cs: ContextShift[IO]
  ): Kleisli[Resource[IO, *], FigurerApp.Deps, PythonProgram] = Kleisli {
    deps =>
      LocalFS.tempDir("figurer".some).evalMap { temp =>
        deps.webdav
          .downloadAllToHost(
            deps.webdav.webdavBase.change(algorithmPath),
            temp.path
          )
          .map(files => PythonProgram(temp, files.toSet))
      }
  }
}
case class PythonProgram(rootDir: BFile, files: Set[BFile])
