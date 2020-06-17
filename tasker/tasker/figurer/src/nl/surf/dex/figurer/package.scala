package nl.surf.dex

import java.io.FileNotFoundException

import better.files.Resource
import cats.effect.IO
import nl.surf.dex.figurer.ProgramStats.Imports
import nl.surf.dex.figurer.program.PythonProgram
import org.python.core.PyList
import org.python.util.PythonInterpreter

package object figurer {

  private val astAnalyzerScript = "imports.py"

  private[figurer] def moduleDeps(program: String): IO[Imports] = {

    val scriptIO = IO.fromEither(
      Resource
        .asString(astAnalyzerScript)
        .toRight(new FileNotFoundException("Can not find necessary resource"))
    )

    for {
      pyInt <- IO(new PythonInterpreter())
      script <- scriptIO
      modules <- IO {
        pyInt.set("program_string", program)
        pyInt.exec(script)
        val list = pyInt.get("modules").asInstanceOf[PyList]
        list.toArray.map(_.asInstanceOf[String]).toSet[String]
      }
    } yield Imports(modules)
  }

  def collectStats(program: PythonProgram): IO[ProgramStats] =
    program.file.foldLeft(IO.pure(ProgramStats.nothing)) {
      case (pStatsIO, file) =>
        file.lineIterator
          .foldLeft(pStatsIO) {
            case (lStatsIO, line) =>
              for {
                lStats <- lStatsIO
                deps <- moduleDeps(line)
              } yield
                lStats.copy(
                  lines = lStats.lines + 1,
                  words = lStats.words + line.split("\\W+").length,
                  chars = lStats.chars + line.length,
                  imports = lStats.imports ++ deps.all
                )
          }
          .map(
            ps =>
              ps.copy(
                contents = ps.contents.updated(
                  program.rootDir.relativize(file).toString,
                  file.contentAsString
                )
            )
          )
    }

}
