package nl.surf.dex.storage.owncloud

import java.net.URI

import com.github.sardine.DavResource
import io.lemonlabs.uri.Uri
import nl.surf.dex.storage.owncloud.WebdavPath.WithToUrl

object WebdavPath {

  trait WithToUrl {
    def toURI: URI
  }

}

case class WebdavPath(serverUri: URI,
                      serverSuffix: String,
                      userPath: Option[String] = None)
    extends WithToUrl {

  def toURI: URI = {
    import io.lemonlabs.uri.typesafe.dsl._

    val uri = Uri(serverUri).toUrl / serverSuffix / userPath
      .map(_.stripSuffix("/"))
      .getOrElse("/")

    uri.toJavaURI
  }

  def change(userPath: String): WebdavPath = copy(userPath = Some(userPath))

  def change(resource: DavResource): WebdavPath =
    copy(
      userPath =
        Some(resource.getPath.stripSuffix("/").replace(serverSuffix, ""))
    )
}
