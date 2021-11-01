/*
 * Copyright 2021 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestaxfrontend.controller

import ltbs.uniform.interpreters.playframework.{PersistenceEngine, UnsafePersistence}
import org.mockito.Mockito.when
import play.api.mvc.AnyContent
import play.api.test.FakeRequest
import play.api.test.Helpers._
import play.modules.reactivemongo.ReactiveMongoApi
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.{RegistrationController, routes}
import uk.gov.hmrc.digitalservicestax.data.TestSampleData.sampleReg
import uk.gov.hmrc.digitalservicestaxfrontend.actions.AuthorisedRequest
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationControllerSpec extends FakeApplicationSpec {

  "RegistrationController.registerAction" must {
    "run the registration journey when there is no registration taking you to first page" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)
      val result = controller.registerAction("").apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 303
      redirectLocation(result) mustBe Some("/global-revenues")
    }
    "take you back to journey controller index when these is a registration" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(
        Some(
          sampleReg
        )
      )
      val result = controller.registerAction("").apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 303
      redirectLocation(result) mustBe Some(routes.JourneyController.index.url)
    }
  }

  "RegistrationController.registrationComplete" must {
    "redirect to registerAction" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)
      val result = controller.registrationComplete.apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 303
      redirectLocation(result) mustBe Some(routes.RegistrationController.registerAction(" ").url)
    }
    "return registration complete" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(
        Some(
          sampleReg
        )
      )
      val result = controller.registrationComplete.apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 200
      contentAsString(result) must include(messagesApi("registration-sent.heading"))
    }
  }

  val controller = new RegistrationController(
    fakeAuthorisedAction,
    httpClient,
    servicesConfig,
    mongo = mock[ReactiveMongoApi],
    interpreter,
    authConnector,
    messagesApi,
    checkYourAnswersRegInstance,
    confirmationRegInstance,
    layoutInstance
    )(
      implicitly
    ) {
    override implicit val persistence : PersistenceEngine[AuthorisedRequest[AnyContent]] = UnsafePersistence()
    override def backend(implicit hc: HeaderCarrier): DSTConnector = mockDSTConnector
  }

}