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

package uk.gov.hmrc.digitalservicestax.controllers

import cats.implicits.catsKernelOrderingForOrder

import javax.inject.{Inject, Singleton}
import ltbs.uniform.UniformMessages
import ltbs.uniform.interpreters.playframework.RichPlayMessages
import play.api.Logger
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.views
import uk.gov.hmrc.digitalservicestax.views.html.{Layout, PayYourDst}
import uk.gov.hmrc.digitalservicestax.actions.Auth
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsController @Inject() (
  authorisedAction: Auth,
  val http: HttpClient,
  val authConnector: AuthConnector,
  servicesConfig: ServicesConfig,
  layout: Layout,
  payYourDst: PayYourDst
)(implicit
  ec: ExecutionContext,
  val messagesApi: MessagesApi,
  val appConfig: AppConfig
) extends ControllerHelpers
    with FrontendHeaderCarrierProvider
    with I18nSupport
    with AuthorisedFunctions {

  val logger                              = Logger(getClass)
  def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  def payYourDST: Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] =
      implicitly[Messages].convertMessagesTwirlHtml(false)

    backend.lookupRegistration().flatMap {
      case None                                          =>
        backend.lookupPendingRegistrationExists().flatMap {
          case true =>
            logger.info("[PaymentsController] Pending registration")
            Future.successful(
              Ok(
                layout(
                  pageTitle = Some(s"${msg("common.title.short")} - ${msg("common.title")}")
                )(views.html.end.pending()(msg))
              )
            )
          case _    => Future.successful(Redirect(routes.RegistrationController.registerAction(" ")))
        }
      case Some(reg) if reg.registrationNumber.isDefined =>
        backend.lookupOutstandingReturns().map { periods =>
          Ok(
            layout(
              pageTitle = Some(s"${msg("common.title.short")} - ${msg("common.title")}")
            )(payYourDst(reg.registrationNumber, periods.toList.sortBy(_.start))(msg))
          )
        }
      case Some(_)                                       =>
        Future.successful(
          Ok(
            layout(
              pageTitle = Some(s"${msg("common.title.short")} - ${msg("common.title")}")
            )(views.html.end.pending()(msg))
          )
        )
    }
  }

}
