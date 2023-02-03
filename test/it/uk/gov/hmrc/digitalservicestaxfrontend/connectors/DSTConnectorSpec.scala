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

package it.uk.gov.hmrc.digitalservicestaxfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import it.uk.gov.hmrc.digitalservicestaxfrontend.util.WiremockServer
import org.scalacheck.Arbitrary.arbitrary
import org.scalactic.anyvals.PosInt
import play.api.libs.json.Json
import play.api.test.Helpers.await
import play.mvc.Http.Status
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data.{CompanyRegWrapper, Period, Postcode, Registration, Return, UTR}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import unit.uk.gov.hmrc.digitalservicestaxfrontend.ConfiguredPropertyChecks
import unit.uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._

import scala.concurrent.Future


class DSTConnectorSpec extends WiremockServer with ConfiguredPropertyChecks {

  implicit override val generatorDrivenConfig = PropertyCheckConfiguration(minSize = 1, minSuccessful = PosInt(1))

  object DSTTestConnector extends DSTConnector(httpClient, servicesConfig) {
    override val backendURL: String = mockServerUrl
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "should submit a registration successfully" in {
    val dstRegNumber = arbitrary[Registration].sample.value

    stubFor(
      post(urlPathEqualTo(s"""/registration"""))
        .willReturn(aResponse().withStatus(200).withBody("{}"))
    )

    val response = DSTTestConnector.submitRegistration(dstRegNumber)
    whenReady(response) { res =>
      res
    }
  }

  "should have correct status code where registration failed" in {
    val dstRegNumber = arbitrary[Registration].sample.value

    stubFor(
      post(urlPathEqualTo("/registration"))
        .willReturn(aResponse().withStatus(Status.BAD_REQUEST).withBody("{}"))
    )

    val submitReg =
      DSTTestConnector
        .submitRegistration(dstRegNumber)
        .recoverWith {
          case _: UpstreamErrorResponse => Future.successful(true)
          case _ => Future.successful(false)
        }
        .map { case b: Boolean => b }

    whenReady(submitReg)(threwException => assert(threwException))
  }

  "should submit a period and a return successfully" in {
    forAll { (period: Period, ret: Return) =>
      val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")

      stubFor(
        post(urlPathEqualTo(s"/returns/$encodedKey"))
          .willReturn(aResponse().withStatus(200).withBody("{}"))
      )

      val response = DSTTestConnector.submitReturn(period, ret)
      whenReady(response) { res =>
        res
      }
    }
  }

  "should have correct status code where submitting return failed" in {
    forAll { (period: Period, ret: Return) =>
      val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")

      stubFor(
        post(urlPathEqualTo(s"/returns/$encodedKey"))
          .willReturn(aResponse().withStatus(Status.BAD_REQUEST).withBody("{}"))
      )

      val submitReturn =
        DSTTestConnector
          .submitReturn(period, ret)
          .recoverWith {
            case _: UpstreamErrorResponse => Future.successful(true)
            case _ => Future.successful(false)
          }
          .map { case b: Boolean => b }

      whenReady(submitReturn)(threwException => assert(threwException))
    }
  }

  "should lookup a company successfully" in {
    forAll { reg: CompanyRegWrapper =>
      stubFor(
        get(urlPathEqualTo(s"/lookup-company"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(reg).toString())
          )
      )

      whenReady(DSTTestConnector.lookupCompany()) { res =>
        res mustBe defined
        res.value mustEqual reg
      }
    }
  }

  "should lookup a company successfully by utr and postcode" in {
    forAll { (utr: UTR, postcode: Postcode, reg: CompanyRegWrapper) =>
      val escaped = postcode.replaceAll("\\s+", "")

      stubFor(
        get(urlPathEqualTo(s"/lookup-company/$utr/$escaped"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(reg).toString())
          )
      )

      val response = DSTTestConnector.lookupCompany(utr, postcode)
      whenReady(response) { res =>
        res mustBe defined
        res.value mustEqual reg
      }
    }
  }

  "should lookup a registration successfully" in {
    forAll { reg: Registration =>
      stubFor(
        get(urlPathEqualTo(s"/registration"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(reg).toString())
          )
      )

      val response = DSTTestConnector.lookupRegistration()
      whenReady(response) { res =>
        res mustBe defined
        res.value mustEqual reg
      }
    }
  }

  "should lookup a list of outstanding return periods successfully" in {
    forAll { periods: Set[Period] =>
      stubFor(
        get(urlPathEqualTo(s"/returns/outstanding"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(periods).toString())
          )
      )

      val response = DSTTestConnector.lookupOutstandingReturns()
      whenReady(response) { res =>
        res must contain allElementsOf periods
      }
    }
  }

  "should lookup a list of submitted return periods successfully" in {
    forAll { periods: Set[Period] =>
      stubFor(
        get(urlPathEqualTo(s"/returns/amendable"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(periods).toString())
          )
      )

      val response = DSTTestConnector.lookupAmendableReturns()
      whenReady(response) { res =>
        res must contain allElementsOf periods
      }
    }
  }

  "should lookup a list of all return periods successfully" in {
    forAll { periods: Set[Period] =>
      stubFor(
        get(urlPathEqualTo(s"/returns/all"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(periods).toString())
          )
      )

      val response = DSTTestConnector.lookupAllReturns()
      whenReady(response) { res =>
        res must contain allElementsOf periods
      }
    }
  }

  "should have Dst registration number when BE call returns 200" in {

    stubFor(
      get(urlPathEqualTo("/tax-enrolment/groupId/12345"))
        .willReturn(aResponse().withStatus(Status.OK).withBody("XYDST0000000000"))
    )

    val response =
      DSTTestConnector
        .getTaxEnrolmentSubscriptionByGroupId("12345")

    whenReady(response) { res =>
      res mustEqual Some("XYDST0000000000")
    }
  }

  "should return None when BE call returns 404" in {

    stubFor(
      get(urlPathEqualTo("/tax-enrolment/groupId/12345"))
        .willReturn(aResponse().withStatus(Status.NOT_FOUND))
    )

    val response =
      DSTTestConnector
        .getTaxEnrolmentSubscriptionByGroupId("12345")

    whenReady(response) { res =>
      res mustEqual None
    }
  }

  "should throw exception when BE call returns 500" in {
    import scala.concurrent.duration._
    val timeout: FiniteDuration = 2.seconds

    stubFor(
      get(urlPathEqualTo("/tax-enrolment/groupId/12345"))
        .willReturn(aResponse().withStatus(Status.INTERNAL_SERVER_ERROR))
    )

    intercept[Exception]{
      await(DSTTestConnector
        .getTaxEnrolmentSubscriptionByGroupId("12345"))(timeout)
    }.getMessage mustEqual "Unexpected error response received while getting Tax enrolment subscription by groupId"
  }

}
