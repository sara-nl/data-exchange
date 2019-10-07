import java.io.{BufferedReader, File, FileOutputStream, FileReader, InputStream, InputStreamReader}
import java.nio.file.{Files, Path, Paths}

import cats.effect.{ExitCode, IO, IOApp, Resource, Timer}
import clients.{SecureContainer, Webdav}
import cats.implicits._

import scala.concurrent.ExecutionContext.Implicits.global

object TestApp extends IOApp {

  implicit val ctx = IO.contextShift(global)

  override def run(args: List[String]): IO[ExitCode] = {

    val remoteCodePath = "demo1_code/good-code.py"
    val remoteDataPath = "demo1_data/data.json"

    Webdav.codeAndDataResources(remoteCodePath, remoteDataPath).use {
      case (tempDir, codeIS, dataIS) =>
        val codeHome = Paths.get(tempDir.toString, "code")
        val dataHome = Paths.get(tempDir.toString, "data")

        val codeDownloadPath = Paths.get(codeHome.toString, remoteCodePath)
        val dataDownloadPath = Paths.get(dataHome.toString, remoteDataPath)

        val downloadIO = for {
          _ <- Webdav.copyStreamToLocalFile(codeIS, codeDownloadPath).start
          _ <- Webdav.copyStreamToLocalFile(dataIS, dataDownloadPath).start
        } yield ()

        for {
          _ <- downloadIO
          containerId <- SecureContainer.createContainer(codeHome, dataHome, remoteCodePath, remoteDataPath)
          _ <- SecureContainer.startContainer(containerId)
          lastStatusOption <- SecureContainer.statusesAndLogs(containerId).compile.last
          _ <- IO {
            println("Last status option" + lastStatusOption)
          }
        } yield ()

    } >> IO(ExitCode.Success)
  }
}
