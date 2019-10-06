package clients

import java.io.InputStream

import cats.effect.IO
import config.TaskerConfig

object Webdav {

  def downloadFile(path: String): IO[InputStream] = IO {
    import com.github.sardine.SardineFactory
    val sardine = SardineFactory.begin(TaskerConfig.webdav.username, TaskerConfig.webdav.password)
    sardine.get(s"${TaskerConfig.webdav.url}/${path}")
  }

}
