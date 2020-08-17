package nl.surf.dex.storage.gdrive

import java.security.MessageDigest
import java.math.BigInteger
package object hash {

  lazy private val md = MessageDigest.getInstance("MD5")

  private[gdrive] def md5(s: String): String = {
    val digest = md.digest(s.getBytes)
    val bigInt = new BigInteger(1, digest)
    val hashedString = bigInt.toString(16)
    hashedString
  }
}
