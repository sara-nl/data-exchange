import java.nio.file.{Files, Path}
import java.util.concurrent.Executors

import cats.effect.{Blocker, IO, Resource}
import org.apache.commons.io.FileUtils

object Resources {

  /**
    * An execution context that is safe to use for blocking operations wrapped into a Resource
    */
  val blockerResource: Resource[IO, Blocker] =
    Resource
      .make(IO(Executors.newCachedThreadPool()))(es => IO(es.shutdown()))
      .map(Blocker.liftExecutorService)

  val tempDirResource: Resource[IO, Path] = Resource.make(
    acquire = IO(Files.createTempDirectory("datex_"))
  )(release = dir => IO(FileUtils.deleteDirectory(dir.toFile)))

}
