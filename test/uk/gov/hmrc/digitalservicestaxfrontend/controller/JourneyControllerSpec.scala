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

import org.mockito.Mockito._
import play.api.i18n.Lang
import play.api.test.Helpers._
import play.api.test._
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.{JourneyController, routes}
import uk.gov.hmrc.digitalservicestax.data.DSTRegNumber
import uk.gov.hmrc.digitalservicestax.data.TestSampleData.sampleReg
import uk.gov.hmrc.digitalservicestaxfrontend.TestInstances
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.mongo.test.MongoSupport

class JourneyControllerSpec extends FakeApplicationSpec {

  "JourneyController" must {

    "redirect to register action when no registration" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)
      val result = controller.index.apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 303
      redirectLocation(result) mustBe Some(routes.RegistrationController.registerAction(" ").url)
    }

    "return pending page with there is a registration without a registration number" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(
        Some(
          sampleReg
        )
      )
      val result = controller.index.apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 200
      implicit val lang = Lang("en")
      contentAsString(result) must include(messagesApi("registration.pending.title"))
    }
    "return Landing page when there is a registration with a registration number" in {
      when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(
        Some(
          sampleReg.copy(
            registrationNumber = Some(DSTRegNumber("ABDST1234567890"))))
      )

      when(mockDSTConnector.lookupOutstandingReturns()) thenReturn Future.successful(
        Set(TestInstances.periodArb.arbitrary.sample.get)
      )

      when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(
        Set(TestInstances.periodArb.arbitrary.sample.get)
      )
      val result = controller.index.apply(
        FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
      )
      status(result) mustBe 200
      contentAsString(result) must include(messagesApi("landing.heading"))
    }
  }

  val controller = new JourneyController(
    fakeAuthorisedAction,
    httpClient,
    authConnector,
    servicesConfig,
    layoutInstance,
    landingInstance
  )(
    implicitly,
    messagesApi,
    appConfig
  ) {
    override def backend(implicit hc: HeaderCarrier): DSTConnector = mockDSTConnector
  }
}



