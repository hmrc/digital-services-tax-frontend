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

package uk.gov.hmrc.digitalservicestax.frontend

import play.api.libs.json.{JsValue, Json}

import scala.io.Source
import scala.util.matching.Regex

class LocationParser(locationsFileSource: String = "/public/location-autocomplete-canonical-list.json") {
  /**
   * @see <a href="https://www.gov.uk/government/publications/open-standards-for-government/country-codes>UK GOV Country Code Standards</a>
   */
  private val alphaTwoCodeRegex: Regex = "^([A-Z]{2}|[A-Z]{2}-[A-Z0-9]{2})$".r

  val locations: List[Location] = readLocations.as[List[Location]].filter(l => l.code.matches(alphaTwoCodeRegex.toString()))

  def name(code: String): String = {
    locations.find(loc => loc.code == code).map(_.name).getOrElse("unknown")
  }

  private def readLocations: JsValue = {
    val source: String = Source.fromInputStream(getClass.getResourceAsStream(locationsFileSource))
      .getLines.mkString

    Json.parse(source)
  }
}