package nl.surf.dex.database

import nl.surf.dex.storage.Share.Location

object Permission {
  sealed trait State extends Product with Serializable {}

  object State {
    case object pending extends State
  }
}

case class Permission(id: Int,
                      // TODO: move all attributes where they belong?
                      algorithmProvider: String,
                      datasetProvider: String,
                      algorithm: Location,
                      algorithmETag: String,
                      dataSet: Location)
