package clients

import java.io.InputStream
import java.nio.file.{Files, Path}

import cats.effect.{IO, Resource}
import config.TaskerConfig
import org.apache.commons.io.FileUtils

object Webdav {

  private val tempDirResource: Resource[IO, Path] = Resource.make(
    acquire = IO(Files.createTempDirectory("datex_"))
  )(release = dir => IO(FileUtils.deleteDirectory(dir.toFile)))

  def downloadFile(path: String): IO[InputStream] = IO {
    import com.github.sardine.SardineFactory
    val sardine = SardineFactory.begin(TaskerConfig.webdav.username, TaskerConfig.webdav.password)
    sardine.get(s"${TaskerConfig.webdav.url}/${path}")
  }

  def codeAndDataResources(remoteCodePath: String, remoteDataPath: String) =
    for {
      dir    <- tempDirResource
      codeIS <- Resource.fromAutoCloseable(Webdav.downloadFile(remoteCodePath))
      dataIS <- Resource.fromAutoCloseable(Webdav.downloadFile(remoteDataPath))
    } yield (dir, codeIS, dataIS)

  def copyStreamToLocalFile(is: InputStream, localPath: Path): IO[Unit] = IO {
    localPath.toFile.getParentFile.mkdirs()
    Files.copy(is, localPath)
    ()
  }

}
