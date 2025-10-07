/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package unit.uk.gov.hmrc.digitalservicestaxfrontend.controller

import play.api.mvc.Results.Redirect
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.digitalservicestax.controllers.AuthenticationController
import uk.gov.hmrc.digitalservicestax.views.html.end.TimeOut
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationServer

import scala.concurrent.Future

class AuthenticationControllerSpec extends FakeApplicationServer {
  val authenticationController = new AuthenticationController(
    mcc,
    app.injector.instanceOf[TimeOut]
  )(appConfig)

  "AuthenticationController.signIn" must {
    "redirect to the gg login page" in {
      val result = authenticationController
        .signIn()
        .apply(
          FakeRequest()
            .withSession()
            .withHeaders(
              "continue"    -> appConfig.dstIndexPage,
              "origin"      -> appConfig.appName,
              "accountType" -> "Organisation"
            )
        )

      status(result) mustBe 303
      redirectLocation(result) mustBe
        redirectLocation(
          Future.successful(
            Redirect(
              url = appConfig.ggLoginUrl,
              queryStringParams = Map(
                "continue"    -> Seq(appConfig.dstIndexPage),
                "origin"      -> Seq(appConfig.appName),
                "accountType" -> Seq("Organisation")
              )
            )
          )
        )
    }
  }

  "AuthenticationController.signOut" must {
    "sign the user out of the service" in {
      val result = authenticationController.signOut().apply(FakeRequest().withSession())

      status(result) mustBe 303
      redirectLocation(result) mustBe Some(appConfig.signOutDstUrl)
    }
  }

  "AuthenticationController.timeIn" must {
    "sign the user back into the service after time out event" in {
      val referrer = "test-referrer"
      val result   = authenticationController
        .timeIn(referrer)
        .apply(
          FakeRequest()
            .withSession()
            .withHeaders(
              "continue"    -> referrer,
              "origin"      -> appConfig.appName,
              "accountType" -> "Organisation"
            )
        )

      status(result) mustBe 303
      redirectLocation(result) mustBe redirectLocation(
        Future.successful(
          Redirect(
            appConfig.ggLoginUrl,
            Map("continue" -> Seq(referrer), "origin" -> Seq(appConfig.appName), "accountType" -> Seq("Organisation"))
          )
        )
      )
    }
  }

  "AuthenticationController.timeOut" must {
    "sign the user out of the service " in {
      val result = authenticationController.timeOut.apply(FakeRequest().withSession())

      status(result) mustBe 200
      contentAsString(result) must include(messagesApi("time-out.title"))
    }
  }
}
