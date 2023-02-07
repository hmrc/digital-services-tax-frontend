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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.actions

import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.{reset, when}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
import org.scalatest.{BeforeAndAfter, BeforeAndAfterEach}
import play.api.http.Status.{OK, SEE_OTHER}
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.mvc.Results.Ok
import play.api.mvc.{AnyContent, AnyContentAsEmpty, Request, Result}
import play.api.test.FakeRequest
import play.api.test.Helpers.{defaultAwaitTimeout, redirectLocation, status}
import play.twirl.api.HtmlFormat
import uk.gov.hmrc.auth.core.Enrolments
import uk.gov.hmrc.digitalservicestax.actions.{AuthorisedRequest, RegistrationAuthFilterProvider}
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.http.HeaderCarrier
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationServer

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationAuthFilterSpec extends FakeApplicationServer with BeforeAndAfterEach {

  private lazy val action                                         = new RegistrationAuthFilterProvider(appConfig, httpClient, servicesConfig) {
    override def backend(implicit hc: HeaderCarrier): DSTConnector = mockDSTConnector
  }
  private val request: FakeRequest[AnyContentAsEmpty.type]        = FakeRequest("", "")
  private val fakeResponse: Request[AnyContent] => Future[Result] = { _ =>
    Future.successful(Ok(HtmlFormat.empty))
  }

  override def guiceApplicationBuilder(): GuiceApplicationBuilder = super
    .guiceApplicationBuilder()
    .configure(Map("feature.dstNewProposedSolution" -> "true"))

  override def beforeEach(): Unit = {
    reset(mockDSTConnector)
    super.beforeEach()
  }

  "RegistrationAuthFilter" must {
    "redirect to lading page when user is already registered to use the DST service" in {
      val authRequest = AuthorisedRequest(InternalId("Int-abc"), Enrolments(Set.empty), request, Some("Id"))
      when(mockDSTConnector.getTaxEnrolmentSubscriptionByGroupId(anyString()))
        .thenReturn(Future.successful(Some("DST")))

      val result = action.invokeBlock(authRequest, fakeResponse)

      status(result)           shouldBe SEE_OTHER
      redirectLocation(result) shouldBe Some("/digital-services-tax")
    }

    "allow request to pass and return status 'Ok' when groupId is 'None'" in {
      val authRequest = AuthorisedRequest(InternalId("Int-abc"), Enrolments(Set.empty), request, None)

      val result = action.invokeBlock(authRequest, fakeResponse)

      status(result) shouldBe OK
    }
  }
}

class RegistrationAuthFilterFeatureFlagFalseSpec extends FakeApplicationServer with BeforeAndAfter {

  private lazy val action                                         = new RegistrationAuthFilterProvider(appConfig, httpClient, servicesConfig)
  private val request: FakeRequest[AnyContentAsEmpty.type]        = FakeRequest("", "")
  private val fakeResponse: Request[AnyContent] => Future[Result] = { _ =>
    Future.successful(Ok(HtmlFormat.empty))
  }

  override def guiceApplicationBuilder(): GuiceApplicationBuilder = super
    .guiceApplicationBuilder()
    .configure(Map("feature.dstNewProposedSolution" -> "false"))

  "RegistrationAuthFilter" must {
    "return Ok when dstNewProposedSolution feature flag is 'false'" in {
      val authRequest = AuthorisedRequest(InternalId("Int-abc"), Enrolments(Set.empty), request, None)

      val result = action.invokeBlock(authRequest, fakeResponse)

      status(result) shouldBe OK
    }
  }
}
