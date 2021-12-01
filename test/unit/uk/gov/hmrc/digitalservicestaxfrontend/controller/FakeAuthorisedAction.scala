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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.controller

import play.api.i18n.MessagesApi
import play.api.mvc._
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data.InternalId
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{Auth, AuthorisedRequest}
import unit.uk.gov.hmrc.digitalservicestaxfrontend.TestInstances

import scala.concurrent.{ExecutionContext, Future}

class FakeAuthorisedAction (mcc: MessagesControllerComponents, val authConnector: AuthConnector)(
  implicit val appConfig: AppConfig, val executionContext: ExecutionContext, val messagesApi: MessagesApi) extends Auth {

  override protected def refine[A](request: Request[A]): Future[Either[Result, AuthorisedRequest[A]]] = {
    val enrolments = TestInstances.enrolmentsArbitrary.arbitrary.sample.get
    Future.successful(Right(AuthorisedRequest(InternalId("Int-abc"), enrolments, request)))
  }

  override def parser: BodyParser[AnyContent] = mcc.parsers.anyContent
}