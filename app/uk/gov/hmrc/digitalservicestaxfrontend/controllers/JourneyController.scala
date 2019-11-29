/*
 * Copyright 2019 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestaxfrontend.controllers

import akka.http.scaladsl.model.headers.LinkParams.title
import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.digitalservicestaxfrontend.aa_data.JsonConversion._
import ltbs.uniform.{UniformMessages, ErrorTree}
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc._
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.AuthorisedFunctions
import uk.gov.hmrc.digitalservicestaxfrontend.aa_data.JourneyState
import uk.gov.hmrc.play.bootstrap.controller.{FrontendController, FrontendHeaderCarrierProvider}
import uk.gov.hmrc.digitalservicestaxfrontend.config.AppConfig
import uk.gov.hmrc.digitalservicestaxfrontend.repo.JourneyStateStore
import uk.gov.hmrc.digitalservicestaxfrontend.views

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class JourneyController @Inject()(
  mcc: MessagesControllerComponents,
  journeyStateStore: JourneyStateStore
)(
  implicit val appConfig: AppConfig,
  ec: ExecutionContext,
  implicit val messagesApi: MessagesApi
)extends ControllerHelpers
  with FrontendHeaderCarrierProvider
  with I18nSupport {

  lazy val interpreter = DSTInterpreter(appConfig, this, messagesApi)
  import interpreter._

  def getState: Future[JourneyState] =
    journeyStateStore.getState("test")

  def setState(in: JourneyState): Future[Unit] =
    journeyStateStore.storeState("test", in)


  def index: Action[AnyContent] = Action.async { implicit request =>
    getState.map { state =>
      implicit val msg: UniformMessages[Html] = interpreter.messages(request)
      Ok(views.html.main_template(
        title =
          s"${msg("common.title.short")} - ${msg("common.title")}"
      )(views.html.hello_world()))
    }
  }

}
