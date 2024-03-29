/*
 * Copyright 2024 HM Revenue & Customs
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

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, LocalDateTime, ZoneId}

import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.{TimeZone, ULocale}

package object frontend {
  private val zone             = "Europe/London"
  val zoneId: ZoneId           = ZoneId.of(zone)
  private val timeFomat        = "h:mma"
  def formattedTimeNow: String = LocalDateTime.now(zoneId).format(DateTimeFormatter.ofPattern(timeFomat)).toLowerCase

  def formatDate(localDate: LocalDate, dateFormatPattern: String = "d MMMM yyyy"): String = {
    val date = java.util.Date.from(localDate.atStartOfDay(zoneId).toInstant)
    createDateFormatForPattern(dateFormatPattern).format(date)
  }

  def createDateFormatForPattern(pattern: String): SimpleDateFormat = {
    val locale: ULocale = ULocale.getDefault
    val sdf             = new SimpleDateFormat(pattern, locale)
    sdf.setTimeZone(TimeZone.getTimeZone(zone))
    sdf
  }

  implicit class RichAddress(value: data.Address) {

    def lines: List[String] = {
      import value._
      val basic = List(
        line1,
        line2.getOrElse(""),
        line3.getOrElse(""),
        line4.getOrElse(""),
        postalCode
      ).filter(_.nonEmpty)

      value match {
        case _: data.UkAddress => basic
        case _                 => basic :+ Location.name(countryCode)
      }
    }

  }

}
