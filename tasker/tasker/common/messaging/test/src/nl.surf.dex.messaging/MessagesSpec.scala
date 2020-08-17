package nl.surf.dex.messaging

import cats.tests.StrictCatsEquality
import io.circe.syntax._
import nl.surf.dex.messaging.Messages.TaskProgress
import nl.surf.dex.messaging.Messages.TaskProgress.State
import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.must.Matchers._

class MessagesSpec extends AnyFunSpec with StrictCatsEquality {

  describe("Messages codecs") {

    it("should encode TaskProgress state properly") {
      import TaskProgress.codecs._
      val data = TaskProgress("123", State.Rejected("For test's sake"))
      val json = data.asJson

      json.hcursor.downField("taskId").as[String] mustEqual Right("123")
      json.hcursor
        .downField("state")
        .downField("name")
        .as[String] mustEqual Right("Rejected")
    }

    it("should encode TaskProgress step state properly") {
      import TaskProgress.codecs._
      val data =
        TaskProgress("123", State.Running(2, Messages.Step.VerifyingAlgorithm))
      val json = data.asJson

      json.hcursor.downField("taskId").as[String] mustEqual Right("123")
      json.hcursor
        .downField("state")
        .downField("name")
        .as[String] mustEqual Right("Running")

      json.hcursor
        .downField("state")
        .downField("currentStepIndex")
        .as[Int] mustEqual Right(2)

      json.hcursor
        .downField("state")
        .downField("currentStep")
        .downField("name")
        .as[String] mustEqual Right("VerifyingAlgorithm")
    }
  }

}
