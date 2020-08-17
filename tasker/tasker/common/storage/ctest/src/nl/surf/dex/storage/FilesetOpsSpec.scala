package nl.surf.dex.storage

import cats.data.NonEmptyList
import cats.effect.{IO, Resource}
import cats.tests.StrictCatsEquality
import nl.surf.dex.storage.FilesetOps.errors.CanNotListException
import nl.surf.dex.testutils.withTmpDir
import nl.surf.dex.storage.Share.NePath
import nl.surf.dex.storage.gdrive.GDriveFileset
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import nl.surf.dex.storage.owncloud.Webdav
import nl.surf.dex.storage.owncloud.config.DexResearchDriveConf
import nl.surf.dex.testutils.implicits._
import org.scalatest.matchers.must.Matchers._
import org.scalatest.funspec.AsyncFunSpec

class FilesetOpsSpec extends AsyncFunSpec with StrictCatsEquality {

  type TestSubj = (CloudStorage, Resource[IO, FilesetOps])

  private val gdriveFilesetOps: TestSubj =
    (CloudStorage.GoogleDrive, Resource.liftF(for {
      conf <- DexGDriveConf.loadIO
      ops = GDriveFileset.make(conf)
    } yield ops))

  private val rdriveFilesetOps: TestSubj = (CloudStorage.ResearchDrive, for {
    conf <- Resource.liftF(DexResearchDriveConf.loadIO)
    sardine <- Webdav.makeSardineR(conf)
  } yield Webdav(sardine, conf))

  describe("Fileset ops") {

    List(gdriveFilesetOps, rdriveFilesetOps).foreach {
      case (label, opsR) =>
        describe(s"in $label") {

          it("should return the listing of a shared nested folder") {
            for {
              remotePath <- NePath.parseIO("nested")
              children <- opsR.use(_.listShareFolder(remotePath))
            } yield {
              children mustEqual List(
                NePath(NonEmptyList("nested", List("1"))),
                NePath(NonEmptyList("nested", List("2")))
              )
            }
          }

          it("should return an error when listing an existing file") {
            recoverToSucceededIf[CanNotListException] {
              opsR
                .use(
                  ops =>
                    NePath
                      .parseIO("coronavirus_dataset.xlsx")
                      .flatMap(ops.listShareFolder)
                )
                .unsafeToFuture()
            }
          }

          it("should return hash of a shared file") {
            for {
              remotePath <- NePath.parseIO("test.py")
              hash <- opsR.use(_.getFilesetHash(remotePath))
            } yield {
              label match {
                case CloudStorage.GoogleDrive =>
                  hash.value mustEqual "fb217d3188688ee8ae40aa407d6f6fed"
                case CloudStorage.ResearchDrive =>
                  hash.value mustEqual "5f2d4deb44a53"
              }
            }
          }

          it("should return hash of a shared folder") {
            for {
              remotePath <- NePath.parseIO("nested")
              hash <- opsR.use(_.getFilesetHash(remotePath))
            } yield {
              label match {
                case CloudStorage.GoogleDrive =>
                  hash.value mustEqual "31db375b78bf8911ec33b3050cf0bae4"
                case CloudStorage.ResearchDrive =>
                  hash.value mustEqual "5f2d230073ea7"
              }
            }
          }

          it("should download a shared non-empty file") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("LICENSE-2.0.txt")
                copiedFile <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                val scannedLocalFiles = tmp.listRecursively.toList
                scannedLocalFiles.length mustEqual 1
                val file = scannedLocalFiles.head
                file mustEqual copiedFile
                file.name mustEqual "LICENSE-2.0.txt"
              }
            }
          }

          it("should download a shared empty file") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("empty.file")
                copiedFile <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                val scannedLocalFiles = tmp.listRecursively.toList
                scannedLocalFiles.length mustEqual 1
                val file = scannedLocalFiles.head
                file mustEqual copiedFile
                file.name mustEqual "empty.file"
              }
            }
          }

          it("should download a shared folder w/empty files") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("empty-files")
                copiedDir <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                val scannedLocalFiles =
                  tmp.listRecursively.filter(_.isRegularFile).toSet
                copiedDir mustEqual tmp / "empty-files"
                scannedLocalFiles.map(_.name) mustEqual Set(
                  "empty1.file",
                  "empty2.file"
                )
              }
            }
          }

          it("should download a flat shared folder") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("kitpes-master")
                copiedDir <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                copiedDir mustEqual tmp / "kitpes-master"
                val files = copiedDir.listRecursively.toSet
                files.map(_.name) mustEqual Set(
                  "LICENSE-2.0.txt",
                  "Pipfile",
                  "Pipfile.lock",
                  "README.md",
                  "requirements.txt",
                  "run.py",
                  "validate.py",
                )
              }
            }
          }

          it("should download a nested folder") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("nested")
                copiedDir <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                copiedDir mustEqual tmp / "nested"
                val scannedLocalFiles = copiedDir.listRecursively.toSet
                  .filter(_.isRegularFile)
                scannedLocalFiles.map(_.name) mustEqual Set(
                  "111.txt",
                  "121.txt",
                  "211.txt"
                )
              }
            }
          }

          it("should download an inner file from a shared folder") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("kitpes-master/LICENSE-2.0.txt")
                copiedFile <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                copiedFile mustEqual tmp / "kitpes-master/LICENSE-2.0.txt"
                copiedFile.isRegularFile mustBe true
                copiedFile.exists mustBe true
                val scannedLocalFiles =
                  tmp.listRecursively.toList.filter(_.isRegularFile)
                scannedLocalFiles mustEqual List(copiedFile)
              }
            }
          }

          it("should download an inner folder from a shared folder") {
            withTmpDir { tmp =>
              for {
                remotePath <- NePath.parseIO("nested/1")
                copiedFile <- opsR.use(_.copySharedFileset(remotePath, tmp))
              } yield {
                copiedFile mustEqual tmp / "nested/1"
                copiedFile.exists mustBe true
                copiedFile.isDirectory mustBe true
                val scannedLocalFiles =
                  tmp.listRecursively.toSet.filter(_.isRegularFile)
                scannedLocalFiles.map(_.name) mustEqual Set(
                  "111.txt",
                  "121.txt"
                )
              }
            }
          }

        }
    }
  }

}
