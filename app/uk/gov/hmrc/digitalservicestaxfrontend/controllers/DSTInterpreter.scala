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

import cats.syntax.semigroup._
import ltbs.uniform.TreeLike.ops._
import ltbs.uniform.common.web.{FormFieldStats, InferFormField}
import ltbs.uniform.interpreters.playframework.{Path, PlayInterpreter, RichPlayMessages, mon}
import ltbs.uniform.{ErrorTree, Input, UniformMessages}
import play.api.mvc.{AnyContent, Request, Results}
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.digitalservicestaxfrontend.config.AppConfig
import uk.gov.hmrc.digitalservicestaxfrontend.views

import scala.concurrent.ExecutionContext.Implicits.global

case class DSTInterpreter(
  appConfig: AppConfig,
  results: Results,
  messagesApi: play.api.i18n.MessagesApi
) extends PlayInterpreter[Html](results) with InferFormField[Html] with Widgets {

  def messages(
    request: Request[AnyContent]
  ): UniformMessages[Html] =
    messagesApi.preferred(request).convertMessagesTwirlHtml() |+|
      UniformMessages.bestGuess.map(HtmlFormat.escape)
  // N.b. this next line very useful for correcting the keys of missing content, leave for now
  //   UniformMessages.attentionSeeker.map(HtmlFormat.escape)
  override def pageChrome(
    keyList: List[String],
    errors: ErrorTree,
    tell: Html,
    ask: Html,
    breadcrumbs: Path,
    request: Request[AnyContent],
    messages: UniformMessages[Html],
    stats: FormFieldStats): Html = {
    val content = views.html.form_wrapper(keyList,
      errors,
      Html(tell.toString + ask.toString),
      breadcrumbs.drop(1),
      stats)(messages, request)
    val errorTitle: String = if(errors.isNonEmpty) s"${messages("common.error")}: " else ""
    views.html.main_template(title =
      errorTitle + s"${messages(keyList.mkString("-") + ".heading")} - ${messages("common.title")}")(
      content)(request, messages, appConfig)
  }

  override def selectionOfFields(
    inner: List[
      (String,
        (List[String],
          Path,
          Input,
          ErrorTree,
          UniformMessages[Html]) ⇒ Html)]
  )(key: List[String],
    path: Path,
    values: Input,
    errors: ErrorTree,
    messages: UniformMessages[Html]): Html = {
    val value: Option[String] =
      values.valueAtRoot.flatMap{_.headOption}
    views.html.uniform.radios(
      key,
      inner.map { _._1 },
      value,
      errors,
      messages,
      inner
        .map {
          case (subkey, f) ⇒
            subkey → f(key :+ subkey,
              path,
              values / subkey,
              errors / subkey,
              messages)
        }
        .filter(_._2.toString.trim.nonEmpty)
        .toMap
    )
  }

}
