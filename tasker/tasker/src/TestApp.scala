import java.io.{BufferedReader, File, FileOutputStream, FileReader, InputStream, InputStreamReader}
import java.nio.file.{Files, Path, Paths}

import cats.effect.{ExitCode, IO, IOApp, Resource, Timer}
import clients.{SecureContainer, Webdav}
import cats.implicits._
import org.apache.commons.io.{FileUtils, FilenameUtils, IOUtils}
import cats.syntax._
import cats.instances.tuple._

import scala.concurrent.ExecutionContext.Implicits.global

object TestApp extends IOApp {

  implicit val ctx = IO.contextShift(global)

  override def run(args: List[String]): IO[ExitCode] = {

    val remoteCodePath = "demo1_code/good-code.py"
    val remoteDataPath = "demo1_data/data.json"

    val tempDirResource: Resource[IO, Path] = Resource.make(
      acquire = IO(Files.createTempDirectory("datex_"))
    )(release = dir => IO(FileUtils.deleteDirectory(dir.toFile)))

    val allResources = for {
      dir <- tempDirResource
      codeIS <- Resource.fromAutoCloseable(Webdav.downloadFile(remoteCodePath))
      dataIS <- Resource.fromAutoCloseable(Webdav.downloadFile(remoteDataPath))
    } yield (dir, codeIS, dataIS)

    def downloadFiles(is: InputStream, localPath: Path): IO[Unit] = IO {
      localPath.toFile.getParentFile.mkdirs()
      Files.copy(is, localPath)
      ()
    }

    allResources.use {
      case (tempDir, codeIS, dataIS) =>
        val codeHome = Paths.get(tempDir.toString, "code")
        val dataHome = Paths.get(tempDir.toString, "data")

        val codeDownloadPath = Paths.get(codeHome.toString, remoteCodePath)
        val dataDownloadPath = Paths.get(dataHome.toString, remoteDataPath)

        val downloadIO = for {
          _ <- downloadFiles(codeIS, codeDownloadPath).start
          _ <- downloadFiles(dataIS, dataDownloadPath).start
        } yield ()

        for {
          _ <- downloadIO
          containerId <- SecureContainer.createContainer(codeHome, dataHome, remoteCodePath, remoteDataPath)
          _ <- SecureContainer.startContainer(containerId)
        } yield ()

    } >> IO(print("Cleaning up resources")) >> IO(ExitCode.Success)
  }
}
