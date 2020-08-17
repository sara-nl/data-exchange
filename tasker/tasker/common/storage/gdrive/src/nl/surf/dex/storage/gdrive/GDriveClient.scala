package nl.surf.dex.storage.gdrive

import cats.data.Kleisli
import cats.effect.IO
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.{Drive, DriveScopes}
import better.files.{File => BFile}
import nl.surf.dex.storage.gdrive.config.DexGDriveConf
import scala.jdk.CollectionConverters._

private[gdrive] object GDriveClient {

  val FolderMimeType = "application/vnd.google-apps.folder"

  private object internals {
    val httpTransport = GoogleNetHttpTransport.newTrustedTransport()
    val scopes = List(DriveScopes.DRIVE)
    val jsonFactory = JacksonFactory.getDefaultInstance
  }

  import internals._

  val makeClientK: Kleisli[IO, DexGDriveConf, Drive] =
    Kleisli { conf =>
      IO(BFile(conf.credentialsFile).newFileInputStream).bracket(
        is =>
          IO(
            new Drive.Builder(
              httpTransport,
              jsonFactory,
              GoogleCredential
                .fromStream(is)
                .createScoped(scopes.asJavaCollection)
            ).setApplicationName(conf.applicationName)
              .build()
        )
      ) { is =>
        IO(is.close())
      }
    }

}
