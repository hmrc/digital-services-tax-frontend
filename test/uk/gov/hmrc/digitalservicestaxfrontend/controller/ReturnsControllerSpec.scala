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

package uk.gov.hmrc.digitalservicestaxfrontend
package controller

import org.mockito.Mockito.when
import org.scalatest.PrivateMethodTester
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers.{status, _}
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.{ReturnsController, routes}
import uk.gov.hmrc.digitalservicestax.data.Period
import uk.gov.hmrc.digitalservicestax.data.TestSampleData.{samplePeriod, sampleReg}
import uk.gov.hmrc.digitalservicestax.views.html.ResubmitAReturn
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec
import uk.gov.hmrc.http.HeaderCarrier
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import uk.gov.hmrc.mongo.test.MongoSupport

class ReturnsControllerSpec extends FakeApplicationSpec with PrivateMethodTester with MongoSupport {

	val returnsController = new ReturnsController(
		fakeAuthorisedAction,
		httpClient,
		servicesConfig,
		mongoComponent,
		interpreter,
		checkYourAnswersRetInstance,
		confirmationRetInstance,
		layoutInstance,
		app.injector.instanceOf[ResubmitAReturn],
		authConnector,
                messagesApi,
                connectors.VolatileReturnsRepoImpl
	)(implicitly) {
	  override def backend(implicit hc: HeaderCarrier): DSTConnector = mockDSTConnector
	}
	private val correctPeriodKey = "0001"
	private val incorrectPeriodKey = "0003"

	"returnsController.showAmendments" must {
		"return a 404 when a registration is not found" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)

			val result = returnsController.showAmendments().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 404 when a registration is found, but amendableReturns are not" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
			when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(Set.empty[Period])

			val result = returnsController.showAmendments().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 200 when a registration and amendableReturns are found" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
			when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(Set(samplePeriod))

			val result = returnsController.showAmendments().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe OK
			contentAsString(result) must include(messagesApi("resubmit-a-return.title"))
		}
	}

	"ReturnsController.postAmendments" must {
		"return a 404 when no amendableReturns are found" in {
			when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(Set.empty[Period])
			val result: Future[Result] = returnsController.postAmendments().apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 400 when the form contains errors" in {
			when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(Set(samplePeriod))
			val incorrectKey = "notPeriodKey"
			val request = FakeRequest()
				.withHeaders("Authorization" -> "Bearer some-token")
				.withFormUrlEncodedBody(incorrectKey -> correctPeriodKey)

			val result: Future[Result] = returnsController.postAmendments().apply(request)

			status(result) mustBe BAD_REQUEST
		}

		"return a 303 when amendableReturns are found" in {
			when(mockDSTConnector.lookupAmendableReturns()) thenReturn Future.successful(Set(samplePeriod))

      val request = FakeRequest()
		      .withHeaders("Authorization" -> "Bearer some-token")
		      .withFormUrlEncodedBody("key" -> correctPeriodKey)

			val result: Future[Result] = returnsController.postAmendments().apply(request)

			status(result) mustBe SEE_OTHER
			redirectLocation(result) mustBe Some(routes.ReturnsController.returnAction(correctPeriodKey, " ").url)
		}

	}

	"ReturnsController.returnAction" must {
		"return a 404 when a registration is not found" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(None)

			val result = returnsController.returnAction(correctPeriodKey).apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 404 when a registration is found, but the returned periodKey does not match what is in the uri" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
			when(mockDSTConnector.lookupAllReturns()) thenReturn Future.successful(Set(samplePeriod))

			val result = returnsController.returnAction(incorrectPeriodKey).apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 303 when a registration is found, periodKey matches what is in the uri and the journey is started" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
			when(mockDSTConnector.lookupAllReturns()) thenReturn Future.successful(Set(samplePeriod))

			val result = returnsController.returnAction("0001").apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe SEE_OTHER
			redirectLocation(result) mustBe Some("/select-activities")

		}
	}
	"ReturnsController.returnComplete" must {
		"return a 404 when the periodKey from lookupAllReturns() does not match the periodKey in the uri" in {
			when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
			when(mockDSTConnector.lookupOutstandingReturns()) thenReturn Future.successful(Set(samplePeriod))
			when(mockDSTConnector.lookupAllReturns()) thenReturn Future.successful(Set(samplePeriod))

			val result = returnsController.returnComplete(incorrectPeriodKey).apply(
				FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
			)
			status(result) mustBe NOT_FOUND
		}

		"return a 200 and the complete page when the periodKey from lookupAllReturns() " +
			"matches the periodKey in the uri" in {
				when(mockDSTConnector.lookupRegistration()) thenReturn Future.successful(Some(sampleReg))
				when(mockDSTConnector.lookupOutstandingReturns()) thenReturn Future.successful(Set(samplePeriod))
				when(mockDSTConnector.lookupAllReturns()) thenReturn Future.successful(Set(samplePeriod))

				val result = returnsController.returnComplete(correctPeriodKey).apply(
					FakeRequest().withSession().withHeaders("Authorization" -> "Bearer some-token")
				)
				status(result) mustBe OK
			  contentAsString(result) must include(messagesApi("confirmation.heading"))
		}
	}
}
