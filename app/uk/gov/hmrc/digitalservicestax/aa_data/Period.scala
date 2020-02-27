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

package uk.gov.hmrc.digitalservicestax.data

import java.time.LocalDate

case class Period(
  start: LocalDate,
  end: LocalDate
)

object Period {

  val firstPeriodStart = LocalDate.of(2020,4,5)

  implicit def pathBinder(r: Registration) = new play.api.mvc.PathBindable[Period] {

    import cats.syntax.either._

    override def bind(key: String, value: String): Either[String, Period] =
      Either.catchOnly[NumberFormatException](r.period(value.toInt)).leftMap(_.getLocalizedMessage).
        flatMap {
          case Some(x) => Right(x)
          case None    => Left(s"${r.company.name} didn't become liable until ${r.dateLiable}")
        }

    override def unbind(key: String, period: Period): String =
      period.start.getYear.toString
  }
}
