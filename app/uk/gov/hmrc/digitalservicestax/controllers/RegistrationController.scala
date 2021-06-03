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

import data._
import config.AppConfig
import connectors.{DSTConnector, MongoPersistence}
import javax.inject.Inject
import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import ltbs.uniform.common.web._
import ltbs.uniform.interpreters.playframework._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.modules.reactivemongo.ReactiveMongoApi
import play.twirl.api.Html

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import cats.instances.future._
import ltbs.uniform.validation.Rule

class RegistrationController @Inject()(
  authorisedAction: AuthorisedAction,
  http: HttpClient,
  servicesConfig: ServicesConfig,
  mongo: ReactiveMongoApi,
  val authConnector: AuthConnector,
  val messagesApi: MessagesApi  
)(implicit
  config: AppConfig,
  ec: ExecutionContext
) extends ControllerHelpers
    with I18nSupport
    with AuthorisedFunctions
    with FrontendHeaderCarrierProvider
    with DSTInterpreter
{

  /// TODO: Make Luke fix rerunOnPriorStateChange
  implicit val futureAdapter = FutureAdapter[Html].alwaysRerun

  private def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  private def hod(id: InternalId)(implicit hc: HeaderCarrier) =
    connectors.CachedDstService(backend)(id)

  private implicit val cyaRegTell = new GenericWebTell[CYA[Registration], Html] {
    override def render(in: CYA[Registration], key: String, messages: UniformMessages[Html]): Html =
      views.html.cya.check_your_registration_answers(s"$key.reg", in.value)(messages)
  }

  private implicit val confirmRegTell = new GenericWebTell[Confirmation[Registration], Html] {
    override def render(in: Confirmation[Registration], key: String, messages: UniformMessages[Html]): Html = {
      val reg = in.value
      views.html.end.confirmation(key: String, reg.companyReg.company.name: String, reg.contact.email: Email)(messages)
    }
  }

  def registerAction(targetId: String): Action[AnyContent] = authorisedAction.async { implicit request: AuthorisedRequest[AnyContent] =>
    import journeys.RegJourney._

    implicit val msg: UniformMessages[Html] = messages(request)

    implicit val persistence: PersistenceEngine[AuthorisedRequest[AnyContent]] =
      MongoPersistence[AuthorisedRequest[AnyContent]](
        mongo,
        collectionName = "uf-registrations",
        config.mongoJourneyStoreExpireAfter        
      )(_.internalId)

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
          Ok(views.html.main_template(
            title =
              s"${msg("registration-sent.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
          )(views.html.end.confirmation("registration-sent", reg.companyReg.company.name, reg.contact.email)(msg)))
        )
    }
  }

  // Members declared in ltbs.uniform.common.web.AutoListingPage
  def renderListPage[A](
    pageKey: List[String],
    breadcrumbs: Breadcrumbs,
    existingEntries: List[ListingRow[Html]],
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    validation: Rule[List[A]]): Html = ???

  // Members declared in ltbs.uniform.common.web.GenericWebInterpreter2
  def unitAsk: WebInteraction[Unit, Html] = ???
  def unitTell: GenericWebTell[Unit,Html] = ???
  // Members declared in ltbs.uniform.common.web.InferFormFields
  def renderAnd(
    pageKey: List[String],
    fieldKey: List[String],
    breadcrumbs: Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    members: Seq[(String, Html)]): Html = ???
  def renderOr(
    pageKey: List[String],
    fieldKey: List[String],
    breadcrumbs: Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    alternatives: Seq[(String, Option[Html])],
    selected: Option[String]): Html = ???
  // Members declared in uk.gov.hmrc.digitalservicestax.controllers.Widgets
  def appConfig: AppConfig = ???

}
