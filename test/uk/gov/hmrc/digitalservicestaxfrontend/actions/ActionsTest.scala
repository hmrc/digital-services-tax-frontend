/*
 * Copyright 2020 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestaxfrontend.actions

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlEqualTo}
import play.api.http.Status
import play.api.mvc.Results
import play.api.test.FakeRequest
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
class ActionsTest extends FakeApplicationSpec {

  "it should test an application" in {

    val authURL = "http://localhost:8500/"
    val localEndpoint = "http://localhost:8080/"

    val action = new AuthorisedAction(mcc, authConnector)(appConfig, global, messagesApi)
//
//    val req = action.invokeBlock(FakeRequest(), {
//      _: AuthorisedRequest[_] => Future.successful(Results.Ok)
//    })
//
//
//    stubFor(
//      post(urlEqualTo(s"$authURL/auth/authorise"))
//        .willReturn(aResponse().withStatus(200).withBody("{}")))
//
//
//    stubFor(
//      post(urlEqualTo(s"$localEndpoint"))
//        .willReturn(aResponse().withStatus(200).withBody("{}")))
//
//
//    whenReady(req) { res =>
//      res.header.status mustEqual Status.OK
//    }
  }
}
