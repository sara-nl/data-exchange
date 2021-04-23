package nl.surf.dex.storage.owncloud

private[owncloud] object OwncloudSharee {

  class ShareeNotFoundException(eppn: String)
      extends RuntimeException(s"Couldn't not find a sharee for eppn=$eppn")
}

/**
  * See: https://doc.owncloud.com/server/developer_manual/core/apis/ocs-recipient-api.html
  */
private[owncloud] case class OwncloudSharee(
    shareType: Byte,
    shareWith: String,
    shareWithAdditionalInfo: String
)
