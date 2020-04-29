/*
 * Copyright 2020 HM Revenue & Customs
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

package object controllers {

  implicit class HtmlEscapingString(val in: String) extends AnyVal {
    import play.twirl.api.HtmlFormat
    def escapeHtml: String = HtmlFormat.escape(in).toString
  }

  implicit class OrderedYesNo(val l: List[String]) extends AnyVal {
    def orderYesNoRadio: List[String] = l match {
      case "None" :: "Some" :: _ if l.length == 2 => l.reverse
      case _ => l
    }
  }

  lazy val fieldNameForSome = "value"

}
