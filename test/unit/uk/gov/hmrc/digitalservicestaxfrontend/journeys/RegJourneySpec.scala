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

import java.time.LocalDate
import ltbs.uniform.interpreters.logictable._
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.digitalservicestax.data._
import unit.uk.gov.hmrc.digitalservicestaxfrontend.data.TestSampleData._
import shapeless.tag.@@
import uk.gov.hmrc.digitalservicestax.data
import uk.gov.hmrc.digitalservicestax.journeys.RegJourney
import unit.uk.gov.hmrc.digitalservicestaxfrontend.data.TestSampleData.sampleContact
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.TestDstService

class RegJourneySpec extends AnyFlatSpec with Matchers {

  implicit val sampleUtrAsk: SampleData[String @@ data.UTR.Tag] = instances(UTR("1234567891"))
  implicit val samplePostcodeAsk: SampleData[String @@ data.Postcode.Tag] = instances(Postcode("BN1 1NB"))
  implicit val sampleDateAsk: SampleData[LocalDate] = instances(LocalDate.of(2021, 3, 31))
  implicit val sampleContactDetailsAsk: SampleData[ContactDetails] = instances(sampleContact)
  implicit val sampleInternationalAddressAsk: SampleData[ForeignAddress] = instances(internationalAddress)
  implicit val sampleUkAddressAsk: SampleData[UkAddress] = instances(sampleAddress)
  implicit val sampleBooleanAsk: SampleData[Boolean] = instances(true)

  implicit val sampleCompanyName: SampleData[String @@ data.CompanyName.Tag] = instancesF {
    case "company-name" => List(CompanyName("Supplied company name"))
    case _ => List(CompanyName("Foo Ltd"))
  }

  "when the global revenue is under £500m we" should "kick the user out" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instances(false)
    assert(LogicTableInterpreter.interpret(RegJourney.registrationJourney(testService)).value.run.isEmpty)
  }

  "when the UK revenue is under £25m we" should "kick the user out" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "uk-revenues" => List(false)
      case _ => List(true)
    }
    assert(LogicTableInterpreter.interpret(RegJourney.registrationJourney(testService)).value.run.isEmpty)
  }

  "when there is a Company from sign in accepting this" should "give you a Registration for that Company " in {
    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(testService)).value.run.asOutcome()
    assert(reg.companyReg.company == sampleCompany)
  }

  "when there is no Company from sign in and the user does not supply a UTR we" should "get a Registration with the supplied Company name" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "check-unique-taxpayer-reference" => List(false)
      case _ => List(true)
    }

    val reg = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      new TestDstService {
        override def lookupCompany(): Option[CompanyRegWrapper] = None
      }.get
    )).value.run.asOutcome()

    reg.companyReg.company.name shouldBe "Supplied company name"
  }

  "when there is no Company from sign in & no supplied UTR & an InternationalAddress is specified we" should "get a Registration with an InternatinalAddress" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "check-unique-taxpayer-reference" => List(false)
      case "check-company-registered-office-address" => List(false)
      case _ => List(true)
    }
    val reg = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      new TestDstService {
        override def lookupCompany(): Option[CompanyRegWrapper] = None
      }.get
    )).value.run.asOutcome()

    reg.companyReg.company.address shouldBe a[ForeignAddress]
  }

  "when there is no Company from sign in & no supplied UTR & a UkAddress is specified we" should "get a Registration with a UkAddress" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "check-unique-taxpayer-reference" => List(true)
      case _ => List(true)
    }

    val reg = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      new TestDstService {
        override def lookupCompany(): Option[CompanyRegWrapper] = None
      }.get
    )).value.run.asOutcome()

    reg.companyReg.company.address shouldBe a[UkAddress]
  }

  "when there is no Company from sign in, but one is found using the UTR, and the user confirms it we" should "get a Registration for that company" in {
    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      new TestDstService {
        override def lookupCompany(): Option[CompanyRegWrapper] = None
      }.get
    )).value.run.asOutcome()

    reg.companyReg.company.name shouldBe utrLookupCompanyName
  }

  "when there is a Company from sign in, saying this is the wrong company" should "kick you out of the journey " in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "confirm-company-details" => List(false)
      case _ => List(true)
    }

    assert(LogicTableInterpreter.interpret(RegJourney.registrationJourney(testService)).value.run.isEmpty)
  }

  "when there is no Company from sign in, and none found for a supplied UTR we" should "ask the user for a companyName and Address" in {
    val reg = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
        new TestDstService {
          override def lookupCompany(): Option[CompanyRegWrapper] = None
          override def lookupCompany(utr: UTR, postcode: Postcode): Option[CompanyRegWrapper] = None
        }.get
      )).value.run.asOutcome()

    reg.companyReg.company.address shouldBe a[UkAddress]
  }

  "when there is no Company from sign in, and the one found by UTR is rejected by the user we" should "be kicked out" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "confirm-company-details" => List(false)
      case _ => List(true)
    }

    assert(LogicTableInterpreter.interpret(RegJourney.registrationJourney(
        new TestDstService {
          override def lookupCompany(): Option[CompanyRegWrapper] = None
        }.get
      )).value.run.isEmpty)
  }

  "the Registration" should "have an alternativeContact when user elected to provide one" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "company-contact-address" => List(false)
      case _ => List(true)
    }

    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      testService
    )).value.run.asOutcome()

    reg.alternativeContact should be ('defined)
  }

  "the Registration" should "not have an alternativeContact when user elected not to provide one" in {
    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      testService
    )).value.run.asOutcome()

    reg.alternativeContact should be ('empty)
  }

  "the Registration" should "have an ultimateParent when the user elected to provide one" in {
    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      testService
    )).value.run.asOutcome()

    reg.ultimateParent should be ('defined)
  }

  "the Registration" should "not have an ultimateParent when the user elected not to provide one" in {
    implicit val sampleBooleanAsk: SampleData[Boolean] = instancesF {
      case "check-if-group" => List(false)
      case _ => List(true)
    }

    val reg: Registration = LogicTableInterpreter.interpret(RegJourney.registrationJourney(
      testService
    )).value.run.asOutcome()

    reg.ultimateParent should be ('empty)
  }

}
