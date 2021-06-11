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

package uk.gov.hmrc.digitalservicestaxfrontend.connectors

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalacheck.Arbitrary.arbitrary
import org.scalactic.anyvals.PosInt
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks
import play.api.libs.json.Json
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data.{CompanyRegWrapper, FinancialTransaction, Period, Postcode, Registration, Return, UTR}
import uk.gov.hmrc.digitalservicestaxfrontend.ConfiguredPropertyChecks
import uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._
import uk.gov.hmrc.http.HeaderCarrier

class DSTConnectorTest extends WiremockSpec with ConfiguredPropertyChecks {

  implicit override val generatorDrivenConfig =
    PropertyCheckConfiguration(minSize = 1, minSuccessful = PosInt(1))

  object DSTTestConnector extends DSTConnector(httpClient, servicesConfig) {
    override val backendURL: String = mockServerUrl
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "should submit a registration successfully" in {
    val dstRegNumber = arbitrary[Registration].sample.value

    stubFor(
      post(urlPathEqualTo(s"""/registration"""))
        .willReturn(aResponse().withStatus(200).withBody("{}")))

    val response = DSTTestConnector.submitRegistration(dstRegNumber)
    whenReady(response) { res =>
      res
    }
  }

  "should submit a period and a return successfully" in {
    forAll { (period: Period, ret: Return) =>

      val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")

      stubFor(
        post(urlPathEqualTo(s"/returns/$encodedKey"))
          .willReturn(aResponse().withStatus(200).withBody("{}")))

      val response = DSTTestConnector.submitReturn(period, ret)
      whenReady(response) { res =>
        res
      }
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

  "should lookup a company successfully by utr and postcode" ignore {
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

  "should lookup a list of financial transactions successfully" ignore {
    forAll { financialTransactions: List[FinancialTransaction] =>

      stubFor(
        get(urlPathEqualTo(s"/financial-transactions"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(financialTransactions).toString())
          )
      )

      val response = DSTTestConnector.lookupFinancialDetails()
      whenReady(response) { res =>
        res must contain allElementsOf financialTransactions
      }
    }
  }

  "should return an empty list when a list of financial transactions cannot be found" in {
    forAll { _: List[FinancialTransaction] =>

      stubFor(
        get(urlPathEqualTo(s"/financial-transactions"))
          .willReturn(
            aResponse()
              .withStatus(404)
          )
      )

      val response = DSTTestConnector.lookupFinancialDetails()
      whenReady(response) { res =>
        res mustBe List.empty
      }
    }
  }
}

