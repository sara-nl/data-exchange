package tasker.webdav

import java.net.URI

import cats.Eq
import com.github.sardine.DavResource
import javax.ws.rs.core.UriBuilder
import tasker.webdav.WebdavPath.WithToUrl

object WebdavPath {

  object implicits {
    implicit val webdavPathEq: Eq[WebdavPath] =
      (x: WebdavPath, y: WebdavPath) => x.toURI.equals(y.toURI)
  }

  trait WithToUrl {
    def toURI: URI
  }

}

case class WebdavPath(serverUri: URI,
                      serverSuffix: String,
                      userPath: Option[String] = None)
    extends WithToUrl {

  def toURI: URI =
    UriBuilder
      .fromUri(serverUri)
      .path(serverSuffix)
      .path(userPath.map(_.stripSuffix("/")).getOrElse("/"))
      .build()

  def change(userPath: String): WebdavPath = copy(userPath = Some(userPath))

  def change(resource: DavResource): WebdavPath =
    copy(
      userPath =
        Some(resource.getPath.stripSuffix("/").replace(serverSuffix, ""))
    )
}
