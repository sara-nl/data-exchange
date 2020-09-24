package nl.surf.dex

import java.io.FileNotFoundException

import better.files.Resource
import cats.effect.IO
import cats.implicits._
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
    program.files
      .foldLeft(ProgramStats.nothing.pure[IO]) {
        case (pStatsIO, file) =>
          val userPath = program.rootDir.relativize(file).toString
          val fileContent = file.contentAsString
          for {
            pStats <- pStatsIO
            pStats <- moduleDeps(file.contentAsString).attempt.map {
              case Right(imports) =>
                pStats.copy(imports = pStats.imports ++ imports.foundImports)
              case Left(reason) =>
                pStats.copy(
                  skippedImports = pStats.skippedImports
                    .updated(userPath, reason.getMessage)
                )
            }
          } yield
            if (fileContent.isEmpty)
              pStats.copy(contents = pStats.contents.updated(userPath, ""))
            else
              pStats.copy(
                lines = pStats.lines + fileContent.split("\r\n|\r|\n").length,
                words = pStats.words + fileContent.split("\\W+").length,
                chars = pStats.chars + fileContent.length,
                contents = pStats.contents.updated(userPath, file.contentAsString)
              )
      }

}
