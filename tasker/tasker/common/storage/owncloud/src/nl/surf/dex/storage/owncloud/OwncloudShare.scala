package nl.surf.dex.storage.owncloud
import cats.implicits._

object OwncloudShare {

  def isFolder(os: OwncloudShare): Boolean = os.item_type === "folder"

  def isAlgorithm(os: OwncloudShare,
                  childrenNames: List[String] = Nil): Boolean =
    childrenNames match {
      case Nil => os.path.endsWith(".py")
      case _   => childrenNames.exists(_ === "run.py")
    }

}

private[owncloud] case class OwncloudShare(
                                           /**
                                             * Internal ID of the storage provider?
                                             */
                                           id: String,
                                           /**
                                             * Email address of the original owner
                                             */
                                           uid_owner: String,
                                           /**
                                             * How is it different from file_target?
                                             */
                                           path: String,
                                           /**
                                             * "folder" or "file"
                                             */
                                           item_type: String,
                                           file_source: Int)
