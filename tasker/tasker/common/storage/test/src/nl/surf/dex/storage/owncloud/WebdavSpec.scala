package nl.surf.dex.storage.owncloud

import cats.effect.{ContextShift, IO}
import cats.implicits._
import nl.surf.dex.storage.config.DexStorageConf
import nl.surf.dex.storage.local.LocalFS
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.must.Matchers._
import better.files.{File => BFile}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class WebdavSpec extends AsyncFunSpec {

  private implicit val cs: ContextShift[IO] = IO.contextShift(global)

  def withTmpDir[X](thunk: BFile => IO[X]) =
    LocalFS
      .tempDir(getClass.getSimpleName.some)
      .use(thunk)

  private implicit def unsafeToFuture[X](thunk: IO[X]): Future[X] =
    thunk.unsafeToFuture()

  val folderName = "WebdavSpec"

  describe("webdav client") {

    it("should return eTag of the folder") {
      for {
        conf <- DexStorageConf.loadIO
        webdavR = WebdavResources.makeWebdavClient.run(conf)
        eTag <- webdavR.use(_.eTag(conf.webdavBase.change(folderName)))
      } yield {
        eTag mustEqual "5ee386d475860"
      }
    }

    it("should recursively download files to the local drive") {
      withTmpDir { tmp =>
        for {
          conf <- DexStorageConf.loadIO
          webdavR = WebdavResources.makeWebdavClient.run(conf)
          fetchedFiles <- webdavR.use(
            _.downloadAllToHost(conf.webdavBase.change(folderName), tmp.path)
          )
        } yield {
          val scannedLocalFiles = (tmp / folderName).listRecursively.toList
            .filter(_.isRegularFile)
          scannedLocalFiles mustEqual fetchedFiles
          scannedLocalFiles.map(_.name) mustEqual List("1.txt", "2.txt")
        }
      }
    }
  }

}
