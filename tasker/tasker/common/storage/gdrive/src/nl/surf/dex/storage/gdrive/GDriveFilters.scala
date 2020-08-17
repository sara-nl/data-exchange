package nl.surf.dex.storage.gdrive

import com.google.api.services.drive.model.{File => GFile}

private object GDriveFilters {

  def topLevel(f: GFile): Boolean = Option(f.getParents).fold(true)(_.isEmpty)

}
