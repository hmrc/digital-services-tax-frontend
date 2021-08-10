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

package uk.gov.hmrc.digitalservicestax.controllers

import ltbs.uniform._
import ltbs.uniform.common.web.{WebAsk, WebTell}
import ltbs.uniform.interpreters.playframework.Breadcrumbs
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestax._
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend.{Kickout, RichAddress}

//@ImplementedBy(classOf[WidgetsImpl])
trait Widgets {

  implicit val appConfig: AppConfig

  implicit val blah3: WebAsk[Html, Nothing] = new WebAsk[Html, Nothing] {
    def decode(out: Input): Either[ErrorTree,Nothing] =
      Left(ErrorMsg("impossible-state").toTree)

    def encode(in: Nothing): Input =
      sys.error("attempting to encode nothing")
    
    def render(
      pageKey: List[String],
      fieldKey: List[String],
      tell: Option[Html],
      breadcrumbs: Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = tell
  }

//  implicit val blah4: WebInteraction[Html,Kickout,Nothing] = new WebInteraction[Html,Kickout,Nothing] {
//    override def apply(
//      id: String,
//      tell: Option[Kickout],
//      defaultIn: Option[Nothing],
//      validationIn: Rule[Nothing],
//      customContent: Map[String, (String, List[Any])]
//    ): WebMonad[Html, Nothing] = new WebMonad[Html, Nothing] {
//      override def apply(pageIn: PageIn[Html])(implicit ec: ExecutionContext): Future[PageOut[Html, Nothing]] =
//        ??? //TODO implement this
//    }
//  }

  implicit val addressTell = new WebTell[Html, Address] {
    override def render(in: Address, key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(Html(
        s"<div id='${key}--sfer-content'>" +
        s"<p>${in.lines.map{x => s"<span class='govuk-body-m'>${x.escapeHtml}</span>"}.mkString("<br/>")}</p>" +
        "</div>"
      ))
  }

  implicit val kickoutTell = new WebTell[Html, Kickout] {
    override def render(in: Kickout, key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(views.html.end.kickout(key)(messages, appConfig))
  }

  implicit val groupCoTell = new WebTell[Html, GroupCompany] {
    override def render(in: GroupCompany, key: String, messages: UniformMessages[Html]): Option[Html] =
      None /// ????
  }

  implicit val companyTell = new WebTell[Html, Company] {
    override def render(in: Company, key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(Html(
        s"<p class='govuk-body-l' id='${key}--sfer-content'>" +
          s"${in.name.toString.escapeHtml}<br>" +
          s"<span class='govuk-body-m'>" +
          s"${in.address.lines.map{_.escapeHtml}.mkString("<br>")}" +
          s"</span>" +
          "</p>"
      ))
  }

  implicit val booleanTell = new WebTell[Html, Boolean] {
    override def render(in: Boolean, key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(Html(in.toString))
  }

}
