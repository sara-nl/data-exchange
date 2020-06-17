package nl.surf.dex.database

object Permission {
  sealed trait State extends Product with Serializable {}

  object State {
    case object pending extends State
  }
}

case class Permission(id: Int,
                      algorithmProvider: String,
                      datasetProvider: String,
                      algorithmPath: String,
                      algorithmETag: String,
                      dataSetPath: String)
