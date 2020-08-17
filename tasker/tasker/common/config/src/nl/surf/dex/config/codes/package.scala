package nl.surf.dex.config

import io.lemonlabs.uri.Url
import io.lemonlabs.uri.config.UriConfig
import pureconfig.ConfigReader
import pureconfig.error.ExceptionThrown

package object codecs {

  implicit val lemonLabScalaUriConfigReader =
    ConfigReader[String].emap(
      (Url
        .parseTry(_: CharSequence)(UriConfig.default))
        .andThen(_.toEither.left.map(ExceptionThrown.apply))
    )

}
