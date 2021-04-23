package nl.surf.dex.storage.owncloud

import cats.effect.{ContextShift, IO}
import cats.tests.StrictCatsEquality
import nl.surf.dex.storage.{CloudStorage, Share}
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import org.http4s.BasicCredentials
import org.http4s.client.blaze.BlazeClientBuilder
import org.scalatest.matchers.must.Matchers._

import scala.concurrent.{ExecutionContext, Future}
import cats.implicits._
import nl.surf.dex.storage.owncloud.OwnCloudShares.Deps
import org.scalatest.funspec.AsyncFunSpec

class OwnCloudSharesSpec extends AsyncFunSpec with StrictCatsEquality {

  private implicit val cs: ContextShift[IO] =
    IO.contextShift(ExecutionContext.global)

  private implicit def unsafeToFuture[X](thunk: IO[X]): Future[X] =
    thunk.unsafeToFuture()

  describe("Owncloud shares service") {

    it("should return all shares") {
      for {
        conf <- DexResearchDriveConf.loadIO
        shares <- OwnCloudShares.getShares.run(
          Deps(
            httpClientR = BlazeClientBuilder[IO](ExecutionContext.global).resource,
            Webdav.makeWebdavClient.run(conf),
            BasicCredentials(conf.webdavUsername, conf.webdavPassword),
            conf
          )
        )
      } yield {
        val foundShares = shares.map(_.path)
        foundShares must not contain "Paris.jpg"
        foundShares must contain("Squirrel.jpg")
        foundShares must contain("ownCloud Manual.pdf")

        val squirellShare = shares.find(_.path eqv "Squirrel.jpg").get
        val manualShare = shares.find(_.path eqv "ownCloud Manual.pdf").get

        squirellShare mustEqual Share(
          CloudStorage.ResearchDrive,
          "Squirrel.jpg",
          isAlgorithm = false,
          isDirectory = false,
          "miccots@gmail.com",
          "https://researchdrive.surfsara.nl/index.php/apps/files/?fileid=275528828"
        )
        manualShare mustEqual Share(
          CloudStorage.ResearchDrive,
          "ownCloud Manual.pdf",
          isAlgorithm = false,
          isDirectory = false,
          "miccots@gmail.com",
          "https://researchdrive.surfsara.nl/index.php/apps/files/?fileid=275528838"
        )
      }
    }
  }

}
