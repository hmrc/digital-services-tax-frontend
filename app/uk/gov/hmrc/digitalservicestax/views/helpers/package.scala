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

package uk.gov.hmrc.digitalservicestax.views.html

import ltbs.uniform.{ErrorTree, UniformMessages}
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend._
import play.twirl.api.{Html, HtmlFormat}
import uk.gov.hmrc.govukfrontend.views.viewmodels.content.HtmlContent
import uk.gov.hmrc.govukfrontend.views.viewmodels.errormessage.ErrorMessage

import collection.immutable.Seq

package object helpers {

  def companyNameAndAddress(company: Company): Html =
    HtmlFormat.fill(Seq(Html(s"<span>${company.name}</span><br/>"), address(company.address)))

  def address(address: Address): Html = {
    val lines = address.lines.map { line =>
      Html(s"<span>$line</span><br/>")
    }
    HtmlFormat.fill(lines)
  }

  def errMessage(
    errors: ErrorTree,
    key: List[String],
    messages: UniformMessages[Html]
  ): Option[ErrorMessage] =
    errors.valueAtRootList.headOption.map { error =>
      ErrorMessage(
        content = HtmlContent(error.prefixWith(key).render(messages))
      )
    }

}
