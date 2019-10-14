package clients.webdav

import java.net.URI

import clients.webdav.WebdavPath.WithToUrl
import com.github.sardine.DavResource
import config.TaskerConfig
import javax.ws.rs.core.UriBuilder

object WebdavPath {

  trait WithToUrl {
    def toURI: URI
  }

  def apply(davResource: DavResource): WebdavPath =
    TaskerConfig.webdav.serverPath.change(davResource)

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
      .path(userPath.getOrElse("/"))
      .build()

  def change(userPath: String): WebdavPath = copy(userPath = Some(userPath))

  def change(resource: DavResource): WebdavPath =
    copy(userPath = Some(resource.getPath.replace(serverSuffix, "")))
}
