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

import cats.implicits._
import ltbs.uniform.common.web._
import ltbs.uniform.interpreters.playframework.{RichPlayMessages, PlayInterpreter2 => PlayInterpreter, mon}
import ltbs.uniform._
import ltbs.uniform.validation.Rule
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.digitalservicestax.views
import uk.gov.hmrc.digitalservicestax.views.html.uniform.radios

trait DSTInterpreter
    extends PlayInterpreter[Html]
    with InferFormFields[Html]
    with AutoListingPage[Html]
    with Widgets {

  def messagesApi: play.api.i18n.MessagesApi

  // TODO implement twirl versions
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
  def unitAsk: WebInteraction[Unit, Html] = new FormField[Unit, Html] {
    override def render(
      pageKey: List[String],
      fieldKey: List[String],
      breadcrumbs: _root_.ltbs.uniform.common.web.Breadcrumbs,
      data: Input,
      errors: ErrorTree,
      messages: UniformMessages[Html]
    ): Option[Html] = None

    override def encode(in: Unit): Input = Input.empty

    override def decode(out: Input): Either[ErrorTree, Unit] = Right(())
  }

  def unitTell: GenericWebTell[Unit, Html] = new GenericWebTell[Unit, Html] {
    def render(in: Unit, key: String, messages: UniformMessages[Html]): Html = Html("")
  }

   def renderAnd(
     pageKey: List[String],
     fieldKey: List[String],
     breadcrumbs: Breadcrumbs,
     data: Input,
     errors: ErrorTree,
     messages: UniformMessages[Html],
     members: Seq[(String, Html)]
   ): Html = Html(
     members.map { x =>
       x._2.toString
     }.mkString
   )

  def renderOr(
    pageKey: List[String],
    fieldKey: List[String],
    breadcrumbs: Breadcrumbs,
    data: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html],
    alternatives: Seq[(String, Option[Html])],
    selected: Option[String]): Html = {
      radios(
        fieldKey,
        alternatives.map{_._1},
        selected,
        errors,
        messages,
        alternatives.collect {
          case (name, Some(html)) => name -> html
        }.toMap
      )
  }

  // def blankTell: Html = Html("")

  def messages(
    request: Request[AnyContent]
  ): UniformMessages[Html] =
    messagesApi.preferred(request).convertMessagesTwirlHtml(escapeHtml = false) |+|
      UniformMessages.bestGuess.map(HtmlFormat.escape)
  // N.b. this next line very useful for correcting the keys of missing content, leave for now
//     UniformMessages.attentionSeeker.map(HtmlFormat.escape)

  override def pageChrome(
    keyList: List[String],
    errors: ErrorTree,
    tell: Option[Html],
    ask: Option[Html],
    breadcrumbs: List[String],
    request: Request[AnyContent],
    messages: UniformMessages[Html]): Html = {

    // very crude method to determine if a page is a kickout - we
    // need a more robust/generic technique based upon different
    // behaviour for 'end' interactions
    def isKickout: Boolean = tell.toString.contains("""<div class="kickout""")

    val content: Html = if (isKickout) {
      tell.getOrElse(throw new Exception("missing tell")) // TODO - review
    } else {
      views.html.form_wrapper(
        keyList,
        errors,
        List(tell, ask).flatten.combineAll,
        List(breadcrumbs.drop(1))
      )(messages, request)
    }

    val errorTitle: String = if(errors.isNonEmpty) s"${messages("common.error")}: " else ""
    views.html.main_template(title =
      errorTitle + s"${messages(keyList.mkString("-") + ".heading")} - ${messages("common.title")} - ${messages("common.title.suffix")}")(
      content)(request, messages, appConfig)
  }

}
