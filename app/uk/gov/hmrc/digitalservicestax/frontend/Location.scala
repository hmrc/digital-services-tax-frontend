/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.digitalservicestax.frontend

import play.api.libs.json._

import scala.io.Source._
import scala.util.matching.Regex

object Location {
  private val alphaTwoCodeRegex: Regex = "^([A-Z]{2}|[A-Z]{2}-[A-Z0-9]{2})$".r

  case class Location(name: String, code: String, `type`: String)

  object Location {
    implicit val LocationReads: Reads[Location] = Json.reads[Location]
  }

  val locationsJson: JsValue = Json.parse(
    fromInputStream(getClass.getResourceAsStream("/public/location-autocomplete-canonical-list.json"))
      .getLines()
      .mkString
  )

  val locations: List[Location] =
    locationsJson.as[List[Location]].filter(l => l.code.matches(alphaTwoCodeRegex.toString()))

  def name(code: String): String = locations.find(x => x.code == code).map(_.name).getOrElse("unknown")
}
