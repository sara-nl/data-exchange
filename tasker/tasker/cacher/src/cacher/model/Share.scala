package cacher.model

object Share {

  case class ShareMetadata(isAlgorithm: Boolean, share: Share)

}

case class Share(id: String,
                 uid_owner: String,
                 path: String,
                 item_type: String,
                 file_target: String)
