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

package uk.gov.hmrc.digitalservicestax
package controllers

import cats.instances.future._
import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import ltbs.uniform.common.web._
import ltbs.uniform.interpreters.playframework._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{Auth, AuthorisedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.http.HttpClient
import views.html.cya._
import views.html.end._

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import data._
import connectors.{DSTConnector, DSTService, MongoUniformPersistence}
import uk.gov.hmrc.digitalservicestax.views.html.Layout
import uk.gov.hmrc.mongo.MongoComponent
import scala.concurrent.duration._

class RegistrationController @Inject()(
  authorisedAction: Auth,
  http: HttpClient,
  servicesConfig: ServicesConfig,
  mongoc: MongoComponent,
  interpreter: DSTInterpreter,
  val authConnector: AuthConnector,
  val messagesApi: MessagesApi,
  cyaReg: CheckYourAnswersReg,
  confirmationReg: ConfirmationReg,
  layout: Layout
)(implicit
  ec: ExecutionContext
) extends ControllerHelpers
    with I18nSupport
    with AuthorisedFunctions
    with FrontendHeaderCarrierProvider
{

  import interpreter._

  val futureAdapter = FutureAdapter.rerunOnStateChange[Html](15.minutes)
  import futureAdapter._

  implicit val persistence: PersistenceEngine[AuthorisedRequest[AnyContent]] =
    new MongoUniformPersistence[AuthorisedRequest[AnyContent]](
      collectionName = "uf-registrations",
      mongoc,
      appConfig.mongoJourneyStoreExpireAfter
    )

  def backend(implicit hc: HeaderCarrier): DSTService[Future] = new DSTConnector(http, servicesConfig)

  private implicit val cyaRegTell = new WebTell[Html, CYA[Registration]] {
    override def render(in: CYA[Registration], key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(cyaReg(s"$key.reg", in.value)(messages))
  }

  private implicit val confirmRegTell = new WebTell[Html, Confirmation[Registration]] {
    override def render(in: Confirmation[Registration], key: String, messages: UniformMessages[Html]): Option[Html] = {
      val reg = in.value
      Some(confirmationReg(key: String, reg.companyReg.company.name: String, reg.contact.email: Email)(messages))
    }
  }

  def registerAction(targetId: String): Action[AnyContent] = authorisedAction.async { implicit request: AuthorisedRequest[AnyContent] =>
    import journeys.RegJourney._

    implicit val msg: UniformMessages[Html] = messages(request)

    backend.lookupRegistration().flatMap {
      case None =>
        interpret(registrationJourney(backend)).run(targetId) { ret => 
          backend.submitRegistration(ret).map { _ => Redirect(routes.RegistrationController.registrationComplete) }
        }

      case Some(_) =>
        Future.successful(Redirect(routes.JourneyController.index))
    }
  }

  def registrationComplete: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] = messages(request)

    backend.lookupRegistration().flatMap {
      case None =>
        Future.successful(
          Redirect(routes.RegistrationController.registerAction(" "))
        )
      case Some(reg) =>
        Future.successful(
          Ok(layout(
            pageTitle = Some(
              s"${msg("registration-sent.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
            )
          )(confirmationReg("registration-sent", reg.companyReg.company.name, reg.contact.email)(msg)))
        )
    }
  }
}
