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

package uk.gov.hmrc.digitalservicestaxfrontend.controller_test

import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar.mock
import uk.gov.hmrc.digitalservicestax.controllers.PaymentsController
import uk.gov.hmrc.digitalservicestaxfrontend.actions.AuthorisedAction
import uk.gov.hmrc.digitalservicestaxfrontend.util.{FakeApplicationSpec, TestDstService}
import uk.gov.hmrc.digitalservicestax.views.html.{Layout, PayYourDst}
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.data.CompanyRegWrapper
import uk.gov.hmrc.digitalservicestax.data.TestSampleData.sampleCompanyRegWrapper
import uk.gov.hmrc.digitalservicestaxfrontend.journeys.testService
import uk.gov.hmrc.digitalservicestaxfrontend.journeys.testService.lookupCompany

import scala.concurrent.Future

class PaymentsControllerSpec extends FakeApplicationSpec {

  //TODO implement tests after we have decided test strategy
  object TestPaymentsController extends PaymentsController(
    authorisedAction = authorisedAction,
    http = httpClient,
    authConnector = authConnector,
    servicesConfig = servicesConfig,
    mongo = reactiveMongoApi,
    layout = layoutInstance,
    payYourDst = payYourDst
  )(implicitly, app.injector.instanceOf[MessagesApi], app.injector.instanceOf[AppConfig])

  "GET /" should {
    "return 200" in {
      new TestDstService {
        override def lookupCompany(): Option[CompanyRegWrapper] = Some(sampleCompanyRegWrapper)
      }.get
      val result: Future[Result] = TestPaymentsController.payYourDST.apply(FakeRequest())
      val resultStatus = status(result)

      println(result)
      println(resultStatus)
      resultStatus mustBe 303
    }
  }
}
