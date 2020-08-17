package nl.surf.dex.storage

sealed trait ShareType

case object Algorithm extends ShareType
case object Data extends ShareType
