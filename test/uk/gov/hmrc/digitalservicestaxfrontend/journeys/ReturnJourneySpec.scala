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

package uk.gov.hmrc.digitalservicestaxfrontend.journeys

import cats.implicits._
import ltbs.uniform.interpreters.logictable._
import org.scalatest.{FlatSpec, Matchers}
import uk.gov.hmrc.digitalservicestax.data._, SampleData._
import uk.gov.hmrc.digitalservicestax.journeys.ReturnJourney
import org.scalatest.{FlatSpec, Matchers}

class ReturnJourneySpec extends FlatSpec with Matchers {

  implicit val sampleMoneyAsk = instances[Money](sampleMoney)
  implicit val sampleActivitySetAsk = instances(sampleActivitySet)
  implicit val sampleRepaymentDetailsAsk = instances(sampleRepaymentDetails)
  implicit val samplePercentAsk = instances(samplePercent)
  implicit val sampleBooleanAsk = instances(true)
  implicit val sampleGroupCompanyListAsk = instances(sampleGroupCompanyList)

  val defaultInterpreter = new TestReturnInterpreter

  "when foo" should "bar" in {
    val r = ReturnJourney.returnJourney(
      defaultInterpreter).value.run.asReturn(true)

    1 shouldBe 1
  }
}
