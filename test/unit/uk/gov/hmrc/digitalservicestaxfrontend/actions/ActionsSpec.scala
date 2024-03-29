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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, post, stubFor, urlPathEqualTo}
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import com.outworkers.util.domain.ShortString
import com.outworkers.util.samplers._
import ltbs.uniform.UniformMessages
import org.scalacheck.Shrink.shrinkAny
import play.api.http.Status
import play.api.libs.json._
import play.api.mvc.{AnyContentAsEmpty, Results}
import play.api.test.FakeRequest
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.{AffinityGroup, CredentialRole, Enrolments}
import uk.gov.hmrc.digitalservicestax.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.digitalservicestax.config.{AppConfig, ErrorHandler}
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.digitalservicestax.views.html.ErrorTemplate
import unit.uk.gov.hmrc.digitalservicestaxfrontend.ConfiguredPropertyChecks
import unit.uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.{FakeApplicationServer, WiremockServer}

import java.net.URI
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ActionsSpec extends FakeApplicationServer with WiremockServer with ConfiguredPropertyChecks {

  // Run wiremock server on local machine with specified port.
  val inet           = new URI(mockServerUrl)
  val wireMockServer = new WireMockServer(wireMockConfig().port(inet.getPort))

  lazy val errorTemplate: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]
  lazy val action                       = new AuthorisedAction(mcc, layoutInstance, fakeAuthConnector)(appConfig, global, messagesApi)

  "it should test an authorised action against auth connector retrievals" in {
    forAll { (enrolments: Enrolments, id: InternalId, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(
        Seq(
          Retrievals.allEnrolments.propertyNames.head  -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
          Retrievals.internalId.propertyNames.head     -> JsString(id),
          Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
          Retrievals.affinityGroup.propertyNames.head  -> Json.toJson(ag)
        )
      )

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(
        FakeRequest(),
        { _: AuthorisedRequest[_] =>
          Future.successful(Results.Ok)
        }
      )

      req.map { res =>
        res.header.status mustEqual Status.OK
      }(global)
    }
  }

  "it should use a standard body parser for actions" in {
    action.parser.getClass mustBe mcc.parsers.anyContent.getClass
  }

  "it should throw an exception in AuthorisedAction if internalId is missing from the retrieval" in {

    forAll { (enrolments: Enrolments, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(
        Seq(
          Retrievals.allEnrolments.propertyNames.head  -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
          Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
          Retrievals.affinityGroup.propertyNames.head  -> Json.toJson(ag)
        )
      )

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(
        FakeRequest(),
        { _: AuthorisedRequest[_] =>
          Future.successful(Results.Ok)
        }
      )

      req.failed.foreach { res =>
        res.getMessage mustEqual "No internal ID for user"
        res mustBe a[RuntimeException]
      }(global)
    }
  }

  "it should throw an exception in AuthorisedAction if the affinity group is not support" in {
    forAll { (enrolments: Enrolments, id: InternalId, role: CredentialRole) =>
      val jsonResponse = JsObject(
        Seq(
          Retrievals.allEnrolments.propertyNames.head  -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
          Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
          Retrievals.internalId.propertyNames.head     -> JsString(id),
          Retrievals.affinityGroup.propertyNames.head  -> JsString(gen[ShortString].value)
        )
      )

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(
        FakeRequest(),
        { _: AuthorisedRequest[_] =>
          Future.successful(Results.Ok)
        }
      )

      req.failed.foreach { res =>
        res mustBe a[JsResultException]
      }(global)
    }
  }

  "it should throw an exception in AuthorisedAction if an invalid internal ID is returned from the retrieval" in {
    forAll { (enrolments: Enrolments, role: CredentialRole, ag: AffinityGroup) =>
      val jsonResponse = JsObject(
        Seq(
          Retrievals.internalId.propertyNames.head     -> JsString("___bla"),
          Retrievals.allEnrolments.propertyNames.head  -> JsArray(enrolments.enrolments.toSeq.map(Json.toJson(_))),
          Retrievals.credentialRole.propertyNames.head -> Json.toJson(role),
          Retrievals.affinityGroup.propertyNames.head  -> Json.toJson(ag)
        )
      )

      stubFor(
        post(urlPathEqualTo(s"/auth/authorise"))
          .willReturn(
            aResponse()
              .withStatus(200)
              .withBody(Json.toJson(jsonResponse).toString())
          )
      )

      val req = action.invokeBlock(
        FakeRequest(),
        { _: AuthorisedRequest[_] =>
          Future.successful(Results.Ok)
        }
      )

      req.failed.foreach { res =>
        res.getMessage mustEqual "Invalid internal ID"
        res mustBe an[IllegalStateException]
      }(global)
    }
  }

  "it should produce an error template using the error handler function" in {
    val handler = app.injector.instanceOf[ErrorHandler]
    val page    = gen[ShortString].value
    val heading = gen[ShortString].value
    val message = gen[ShortString].value

    implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()

    val msg = handler.standardErrorTemplate(
      page,
      heading,
      message
    )
    import ltbs.uniform.interpreters.playframework._

    implicit val messages: UniformMessages[Html] =
      messagesApi.preferred(req).convertMessagesTwirlHtml()

    implicit val conf: AppConfig = appConfig

    msg mustEqual errorTemplate(page, heading, message)
  }
}
