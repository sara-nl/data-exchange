package tasker.webdav

import java.net.URI

import com.github.sardine.DavResource
import javax.ws.rs.core.UriBuilder
import tasker.config.TaskerConfig
import tasker.webdav.WebdavPath.WithToUrl

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
