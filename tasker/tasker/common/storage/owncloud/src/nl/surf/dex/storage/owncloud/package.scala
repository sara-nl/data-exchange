package nl.surf.dex.storage

import cats.Eq
import com.github.sardine.DavResource
import nl.surf.dex.storage.Fileset.Hash

package object owncloud {
  object implicits {

    /**
      * Syntactic sugar for stripping quotes from Owncloud's ETag
      */
    implicit class DavResourceWithConversions(val r: DavResource)
        extends AnyVal {
      def getSafeHash: Hash =
        Hash(r.getEtag.stripSuffix("\"").stripPrefix("\""))
    }

    /**
      * Safe comparison of [[WebdavPath]]-s
      */
    implicit val webdavPathEq: Eq[WebdavPath] =
      (x: WebdavPath, y: WebdavPath) => x.toURI.equals(y.toURI)

  }

}
