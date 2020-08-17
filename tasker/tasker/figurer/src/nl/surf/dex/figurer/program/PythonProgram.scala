package nl.surf.dex.figurer.program
import better.files.{File => BFile}
import cats.effect.IO
import cats.implicits._

object PythonProgram {

  def apply(file: BFile): IO[PythonProgram] = {
    if (file.isDirectory)
      PythonProgram(file, file.listRecursively.toSet).pure[IO]
    else if (file.name.endsWith(".py"))
      PythonProgram(file.parent, Set(file)).pure[IO]
    else
      IO.raiseError(
        new IllegalArgumentException(s"$file is not a Python program")
      )
  }
}
case class PythonProgram(rootDir: BFile, files: Set[BFile])
