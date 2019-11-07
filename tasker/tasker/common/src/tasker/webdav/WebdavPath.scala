package tasker.webdav

import java.net.URI

import cats.Eq
import com.github.sardine.DavResource
import javax.ws.rs.core.UriBuilder
import tasker.config.TaskerConfig
import tasker.webdav.WebdavPath.WithToUrl

object WebdavPath {

  object implicits {
    implicit val webdavPathEq: Eq[WebdavPath] =
      (x: WebdavPath, y: WebdavPath) => x.toURI.equals(y.toURI)
  }

  trait WithToUrl {
    def toURI: URI
  }

  def apply(davResource: DavResource): WebdavPath =
    TaskerConfig.webdav.serverPath.change(davResource)

  /**
    * Returns a WebDav path with the default server base path.
    */
  def apply(userPath: String): WebdavPath =
    TaskerConfig.webdav.serverPath.change(userPath)

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
