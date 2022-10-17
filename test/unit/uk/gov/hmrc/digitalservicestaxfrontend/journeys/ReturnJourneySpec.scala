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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.journeys

import ltbs.uniform.interpreters.logictable._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.digitalservicestax.data.Activity.SocialMedia
import unit.uk.gov.hmrc.digitalservicestaxfrontend.data.TestSampleData._
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.journeys.ReturnJourney

import scala.util.matching.Regex

class ReturnJourneySpec extends AnyFlatSpec with Matchers {

  val lossKeys: Regex = {s"^report-(" + Activity.values.map(Activity.toUrl).mkString("|") + ")-loss"}.r

  implicit val sampleMoneyAsk = instances[Money](sampleMoney)
  implicit val sampleActivitySetAsk = instances(sampleActivitySet)
  implicit val sampleRepaymentDetailsAsk = instances(sampleRepaymentDetails)
  implicit val samplePercentAsk = instances(samplePercent)
  implicit val sampleGroupCompnay = instances(sampleGroupCompany)
  implicit val sampleGroupCompanyListAsk = instances(sampleGroupCompanyList)
  implicit val sampleBooleanAsk = instancesF {
    case lossKeys(_) => List(false)
    case _ => List(true)
  }

  implicit val sampleListQty = SampleListQty[GroupCompany](1)

  "If the registration is not for a group we" should "not see manage-companies questions" in {
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()

    ret.companiesAmount shouldBe Symbol("empty")
  }

  "If the registration is for a group we" should "see manage-companies questions" in {
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleRegWithParent
    )).value.run.asOutcome()

    ret.companiesAmount shouldBe Symbol("nonEmpty")
  }

  "Return.alternativeCharge length" should "be the same as length of reported activities" in {
    implicit val sampleActivitySetAsk = instances(Set[Activity](SocialMedia))
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.alternateCharge.size shouldBe 1
  }

  "Return.alternateCharge " should "be empty when no alternate charge is reported" in {
    implicit val sampleBooleanAsk = instancesF {
      case "report-alternative-charge" => List(false)
      case _ => List(true)
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.alternateCharge  should be (Symbol("empty"))
  }

  "Return.alternateCharge " should "not be empty when an alternate charge is reported" in {
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()

    ret.alternateCharge  should be (Symbol("nonEmpty"))
  }

  "Return.crossBorderReliefAmount" should "be zero when report-cross-border-transaction-relief is false" in {
    implicit val sampleBooleanAsk = instancesF {
      case "report-cross-border-transaction-relief" => List(false)
      case _ => List(true)
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()

    ret.crossBorderReliefAmount shouldEqual BigDecimal(0)
  }

  "Return.repayment" should "be nonEmpty when the user has asked for a repayment" in {
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()

    ret.repayment  should be (Symbol("nonEmpty"))
  }

  "Return.repayment" should "be empty when the user has not asked for a repayment" in {
    implicit val sampleBooleanAsk = instancesF {
      case "repayment" => List(false)
      case _ => List(true)
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.repayment  should be (Symbol("empty"))
  }

  "Return.allowanceAmount" should "not be asked when activity doesn't apply alternative charge" in {
    // social media only
    implicit val sampleActivitySetAsk = instances(Set[Activity](SocialMedia))
    // no to alternative charge
    implicit val sampleBooleanAsk = instancesF {
      case "report-social-media-alternative-charge" => List(false)
      case _ => List(true)
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.allowanceAmount should be (Symbol("empty"))
  }

  "Return.allowanceAmount" should "not be asked when 0 Percentage supplied for activity" in {
    // social media only
    implicit val sampleActivitySetAsk: SampleData[Set[Activity]] = instances(Set[Activity](SocialMedia))
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "report-social-media-loss" => List(false)
      case _ => List(true)
    }
    implicit val samplePercentAsk = instancesF {
      case "report-social-media-operating-margin" => List(Percent(0))
      case _ => List.empty[Percent]
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.allowanceAmount should be (Symbol("empty"))
  }

  "Return.allowanceAmount" should "be asked when activity does apply alternative charge" in {
    // social media only
    implicit val sampleActivitySetAsk = instances(Set[Activity](SocialMedia))
    // no to alternative charge
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.allowanceAmount should be (Symbol("nonEmpty"))
  }

  "Return.allowanceAmount" should "be asked when positve Percentage supplied for activity" in {
    // social media only
    implicit val sampleActivitySetAsk: SampleData[Set[Activity]] = instances(Set[Activity](SocialMedia))
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "report-social-media-loss" => List(false)
      case _ => List(true)
    }
    val ret:Return = LogicTableInterpreter.interpret(ReturnJourney.returnJourney(
      samplePeriod,
      sampleReg
    )).value.run.asOutcome()
    ret.allowanceAmount should be (Symbol("nonEmpty"))
  }
}
