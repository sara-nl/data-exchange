package nl.surf.dex.storage.owncloud
import cats.implicits._
import better.files.{File => BFile}

object OwncloudShare {

  def isFolder(os: OwncloudShare): Boolean = os.item_type === "folder"

  def isAlgorithm(
      os: OwncloudShare,
      childrenUserPaths: List[String] = Nil
  ): Boolean =
    childrenUserPaths match {
      case Nil => os.path.endsWith(".py")
      case _   => childrenUserPaths.exists(cup => BFile(cup).name === "run.py")
    }

}

private[owncloud] case class OwncloudShare(
    /**
      * Internal ID of the storage provider?
      */
    id: String,
    /**
      * Username of the share owner
      */
    uid_owner: String,

    /**
      * Email address of the share owner
      */
    additional_info_owner: Option[String],
    /**
      * How is it different from file_target?
      */
    path: String,
    /**
      * "folder" or "file"
      */
    item_type: String,
    file_source: Int
)
