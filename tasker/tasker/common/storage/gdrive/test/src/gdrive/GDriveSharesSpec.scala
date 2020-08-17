package gdrive

import cats.implicits._
import cats.tests.StrictCatsEquality
import nl.surf.dex.storage.gdrive.GDriveShares
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.{CloudStorage, Share}
import nl.surf.dex.testutils.implicits._
import org.scalatest.funspec.AsyncFunSpec
import org.scalatest.matchers.must.Matchers._

class GDriveSharesSpec extends AsyncFunSpec with StrictCatsEquality {

  describe("GDrive shares service") {

    /**
      * There are some shares pre-configured in Google Drive.
      * See README.md for more.
      */
    it("should return pre-configured shares with correct values") {
      for {
        conf <- DexGDriveConf.loadIO
        shares <- GDriveShares.getShares.run(conf)
      } yield {
        val foundShares = shares.map(_.path)
        foundShares must not contain ("README.md")
        foundShares must contain("kitpes-master")
        foundShares must contain("coronavirus_dataset.xlsx")
        foundShares must contain("test.py")

        val kitpesShare = shares.find(_.path eqv "kitpes-master").get
        val covidShare = shares.find(_.path eqv "coronavirus_dataset.xlsx").get
        val testShare = shares.find(_.path eqv "test.py").get

        kitpesShare must equal(
          Share(
            CloudStorage.GoogleDrive,
            "kitpes-master",
            isAlgorithm = true,
            isDirectory = true,
            "miccots@gmail.com",
            "https://drive.google.com/drive/folders/1XHSoThsyo-z9EyHrPo5d_6C2tC5Yxd6R"
          )
        )

        covidShare must equal(
          Share(
            CloudStorage.GoogleDrive,
            "coronavirus_dataset.xlsx",
            isAlgorithm = false,
            isDirectory = false,
            "miccots@gmail.com",
            "https://drive.google.com/file/d/1ecKby5pwlwU0FUCJEY0QqbHkedT-4DbO/view?usp=drivesdk"
          )
        )
        testShare must equal(
          Share(
            CloudStorage.GoogleDrive,
            "test.py",
            isAlgorithm = true,
            isDirectory = false,
            "miccots@gmail.com",
            "https://drive.google.com/file/d/1QRZhUSKxqw7MtQDZQU8KeEt3BY3sdYux/view?usp=drivesdk"
          )
        )
      }
    }

  }
}
