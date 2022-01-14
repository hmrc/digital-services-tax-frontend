/*
 * Copyright 2022 HM Revenue & Customs
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

import org.mockito.Mockito.when
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.{PaymentsController, routes}
import uk.gov.hmrc.digitalservicestax.data.DSTRegNumber
import uk.gov.hmrc.digitalservicestax.views.html.PayYourDst
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.digitalservicestaxfrontend.data.TestSampleData.{samplePeriod, sampleReg}
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PaymentsControllerSpec extends FakeApplicationServer {

	val paymentsController: PaymentsController = new PaymentsController(
		fakeAuthorisedAction,
		httpClient,
		authConnector,
		servicesConfig,
		layoutInstance,
		app.injector.instanceOf[PayYourDst]
	)(implicitly, messagesApi, appConfig) {
		override def backend(implicit hc: HeaderCarrier): DSTConnector = mockDSTConnector
	}

	"PaymentsController.payYourDST" must {
		"redirect back to the start of the journey if a registration isn't found" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)
			val result = paymentsController.payYourDST().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe 303
			redirectLocation(result) mustBe Some(routes.RegistrationController.registerAction(" ").url)
		}

		"show the payYourDST page when return periods and registrationNumber is defined" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(
				Some(
					sampleReg.copy(
						registrationNumber = Some(DSTRegNumber("ABDST1234567890"))
					)
				)
			)
			when(mockDSTConnector.lookupOutstandingReturns()) thenReturn Future.successful(
				Set(samplePeriod)
			)

			val result = paymentsController.payYourDST().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe 200
			contentAsString(result) must include(messagesApi("pay-your-dst.heading"))
		}

		"show the pending page when a registrationNumber is not defined" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))

			val result = paymentsController.payYourDST().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe 200
			contentAsString(result) must include(messagesApi("registration.pending.title"))
		}
	}

}
