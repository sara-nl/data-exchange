package nl.surf.dex

import cats.Eq
import nl.surf.dex.storage.Share.Location

package object storage {

  object implicits {

    /**
      * Safe comparison of [[Location]]-s
      */
    implicit val webdavPathEq: Eq[Location] =
      (x: Location, y: Location) => x.path.eq(y.path)
  }
}
