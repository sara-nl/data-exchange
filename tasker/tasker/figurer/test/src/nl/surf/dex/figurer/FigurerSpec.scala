package nl.surf.dex.figurer

import java.nio.file.NoSuchFileException

import better.files.{Resource, File => BFile}
import cats.effect.IO
import cats.implicits._
import nl.surf.dex.figurer.program.PythonProgram
import org.scalatest.funspec.AsyncFunSpecLike
import org.scalatest.matchers.should.Matchers._

class FigurerSpec extends AsyncFunSpecLike {

  describe("figurer") {
    it("should return an error when given a non-existing file") {
      recoverToSucceededIf[NoSuchFileException] {
        (for {
          program <- PythonProgram(BFile("/tmp"), BFile("trulalala.py"))
          stats <- collectStats(program)
        } yield stats).unsafeToFuture()
      }
    }
    it("should return an error when given a non-py file") {
      recoverToSucceededIf[IllegalArgumentException] {
        val file = BFile(Resource.getUrl("notpython.js"))
        PythonProgram(file.parent, file).unsafeToFuture()
      }
    }

    it("should return stats with zeroes when given an empty file") {
      val file = BFile(Resource.getUrl("empty.py"))
      (for {
        program <- PythonProgram(file.parent, file)
        stats <- collectStats(program)
      } yield stats).unsafeToFuture().map { res =>
        res shouldEqual ProgramStats.nothing
          .copy(contents = Map("empty.py" -> ""))
      }
    }

    it("should include files contents into the stats") {
      val file = BFile(Resource.getUrl("helloworld.py"))
      (for {
        program <- PythonProgram(file.parent, file)
        stats <- collectStats(program)
      } yield stats).unsafeToFuture().map { res =>
        res.contents shouldEqual Map(
          "helloworld.py" -> "print(\"Hello world\")"
        )
      }
    }

    it("should return lines count for a non-empty file") {
      val file = BFile(Resource.getUrl("helloworld.py"))
      (for {
        program <- PythonProgram(file.parent, file)
        stats <- collectStats(program)
      } yield stats).unsafeToFuture().map { res =>
        res.lines shouldEqual 1
        res.words shouldEqual 3
        res.chars shouldEqual 20
        res.imports shouldEqual Set.empty
      }
    }

    it("should return good stats for a single-dir project") {
      val root = BFile(Resource.getUrl("kitpes"))
      val program = PythonProgram(root, root.list.toSet)
      (for {
        stats <- collectStats(program)
      } yield stats).unsafeToFuture().map { res =>
        res.lines shouldEqual 422
        res.words shouldEqual 2320
        res.chars shouldEqual 16816
        res.imports shouldEqual Set(
          "os",
          "os.path",
          "numpy",
          "__future__",
          "tensorflow.keras.preprocessing.image",
          "tensorflow",
          "sys",
          "keras.preprocessing.image"
        )
      }
    }

    it("should de-duplicate packages") {
      val file1 = BFile(Resource.getUrl("imports-scattered.py"))
      val file2 = BFile(Resource.getUrl("imports-top.py"))
      (for {
        program <- PythonProgram(file1.parent, Set(file1, file2)).pure[IO]
        stats <- collectStats(program)
      } yield stats).unsafeToFuture().map { res =>
        res.imports.count(c => c.equals("os")) shouldEqual 1
      }
    }

    it("should resolve an import") {
      val program = "import foo"
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set("foo")
      }
    }

    it("should resolve an import as") {
      val program = "import foo as bar"
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set("foo")
      }
    }
    it("should resolve non-top level imports") {
      val program =
        """
          |try:
          |    from http.client import responses # For Python 3
          |except ImportError:  # For Python 2.5-2.7
          |    from httplib import responses  # NOQA""".stripMargin
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set("http.client", "httplib")
      }
    }
    it("should resolve a partial import") {
      val program = "from time import gmtime, strftime"
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set("time")
      }
    }
    it("should find no imports in an empty line") {
      val program = ""
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set.empty
      }
    }
    it("should find no imports in a line with a non-import statement") {
      val program = "print('Hello')"
      moduleDeps(program).unsafeToFuture().map { res =>
        res.foundImports shouldEqual Set.empty
      }
    }

  }

}
