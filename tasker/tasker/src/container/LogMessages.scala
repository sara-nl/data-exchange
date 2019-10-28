package container

object LogMessages {

  def largeOutputWithNewlines(outputLabel: String, content: String): String =
    content match {
      case "" =>
        s"----- Ø content for $outputLabel -----"
      case _ =>
        s"----- Start of $outputLabel -----\n $content \n ----- End of $outputLabel -----"
    }

}
