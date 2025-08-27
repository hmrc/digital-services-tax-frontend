/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestax.connectors

import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.digitalservicestax.data.BackendAndFrontendJson._
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.http.HttpReads.Implicits._
import uk.gov.hmrc.http._
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import java.net.URI
import scala.concurrent.{ExecutionContext, Future}

class DSTConnector(val http: HttpClientV2, servicesConfig: ServicesConfig)(implicit
  executionContext: ExecutionContext,
  hc: HeaderCarrier
) extends DSTService[Future] {

  val backendURL: String = servicesConfig.baseUrl("digital-services-tax") + "/digital-services-tax"

  def submitRegistration(reg: Registration): Future[HttpResponse] =
    http
      .post(new URI(s"$backendURL/registration").toURL)
      .withBody(Json.toJson(reg))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(value) => value
        case Left(e)      => throw e
      }

  def submitReturn(period: Period, ret: Return): Future[HttpResponse] = {
    val encodedKey = java.net.URLEncoder.encode(period.key, "UTF-8")
    http
      .post(new URI(s"$backendURL/returns/$encodedKey").toURL)
      .withBody(Json.toJson(ret))
      .execute[Either[UpstreamErrorResponse, HttpResponse]]
      .map {
        case Right(value) => value
        case Left(e)      => throw e
      }
  }

  def lookupCompany(): Future[Option[CompanyRegWrapper]] =
    http.get(new URI(s"$backendURL/lookup-company").toURL).execute[Option[CompanyRegWrapper]]

  def lookupCompany(utr: UTR, postcode: Postcode): Future[Option[CompanyRegWrapper]] = {
    http.get(new URI(s"$backendURL/lookup-company/$utr/${postcode.mkString("").replace(" ", "")}").toURL).execute[Option[CompanyRegWrapper]]
  }

  def lookupRegistration(): Future[Option[Registration]] =
    http.get(new URI(s"$backendURL/registration").toURL).execute[Option[Registration]]

  def lookupPendingRegistrationExists(): Future[Boolean] =
    http.get(new URI(s"$backendURL/pending-registration").toURL).execute[HttpResponse].map {
      case resp if resp.status == OK => true
      case _                         => false
    }

  def lookupOutstandingReturns(): Future[Set[Period]] =
    http.get(new URI(s"$backendURL/returns/outstanding").toURL).execute[List[Period]].map(_.toSet)

  def lookupAmendableReturns(): Future[Set[Period]] =
    http.get(new URI(s"$backendURL/returns/amendable").toURL).execute[List[Period]].map(_.toSet)

  def lookupAllReturns(): Future[Set[Period]] =
    http.get(new URI(s"$backendURL/returns/all").toURL).execute[List[Period]].map(_.toSet)

  case class MicroServiceConnectionException(msg: String) extends Exception(msg)
}
