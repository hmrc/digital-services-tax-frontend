package uk.gov.hmrc.digitalservicestaxfrontend.actions

import org.scalatest.FlatSpec
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec

class ActionsTest extends FakeApplicationSpec {

  "it should test an application" in {
    val action = new AuthorisedAction(mcc)(appConfig)()
  }
}
