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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.data

import cats.implicits._
import cats.kernel.Monoid
import org.scalacheck.Gen
import org.scalactic.anyvals.PosInt
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend._
import unit.uk.gov.hmrc.digitalservicestaxfrontend.ConfiguredPropertyChecks
import unit.uk.gov.hmrc.digitalservicestaxfrontend.TestInstances._

import java.time.LocalDate

class ValidatedTypeSpec extends AnyFlatSpec with Matchers with ConfiguredPropertyChecks {

  it should "fail to parse a validated tagged type using an of method" in {
    intercept[IllegalArgumentException] {
      Postcode("this is not a postcode")
    }
  }

  it should "concatenate lines in a UKAddress value" in {
    forAll { ukAddress: UkAddress =>
      ukAddress.lines shouldEqual List(
        ukAddress.line1,
        ukAddress.line2.getOrElse(""),
        ukAddress.line3.getOrElse(""),
        ukAddress.line4.getOrElse(""),
        ukAddress.postalCode
      ).filter(_.nonEmpty)
    }
  }

  it should "validate IBAN numbers from a series of concrete examples" in {
    forAll(Gen.oneOf(ibanList), minSuccessful(PosInt(86))) { source: String =>
      IBAN.of(source) shouldBe defined
    }
  }

  it should "concatenate lines in a ForeignAddress value" in {
    forAll { foreignAddress: ForeignAddress =>
      foreignAddress.lines shouldEqual List(
        foreignAddress.line1,
        foreignAddress.line2.getOrElse(""),
        foreignAddress.line3.getOrElse(""),
        foreignAddress.line4.getOrElse(""),
        Location.name(foreignAddress.countryCode)
      ).filter(_.nonEmpty)
    }
  }

  it should "return an appropriate paymentDue date" in {
    forAll { (p1:Period, p2:Period) =>
      List(p1,p2).sortBy(_.end) shouldEqual List(p1, p2).sortBy(_.paymentDue)
    }
    forAll { p:Period =>
      p.paymentDue > p.end.plusMonths(9) &&
        p.paymentDue < p.end.plusMonths(10).withDayOfMonth(2) shouldBe true
    }
  }

  it should "correctly define the class name of a validated type" in {
    AccountNumber.className shouldEqual "AccountNumber$"
  }

  it should "store percentages as floats and initialise percent monoids with float monoids" in {
    Monoid[Percent].empty shouldEqual Monoid[Float].empty
  }

  it should "add up percentages using monoidal syntax" in {

    val generator = for {
      p1 <- Gen.chooseNum[Float](0F, 100F)
      p2 = 100F - p1
    } yield p1 -> p2

    forAll(generator) { case (p1, p2) =>
      whenever(p1 >= 0F && p2 >= 0F && BigDecimal(p1.toString).scale <= 3 && BigDecimal(p2.toString).scale <= 3) {
        val addedPercent = Monoid.combineAll(Seq(Percent(p1), Percent(p2)))
        val addedBytes = Monoid.combineAll(Seq(p1, p2))
        addedPercent shouldEqual Percent(addedBytes)
      }
    }
  }

  it should "fail to construct a type with a scale of more than 3" in {
    an[IllegalArgumentException] should be thrownBy Percent.apply(1.1234F)
  }

  it should "allow comparing local dates with cats syntax" in {
    import cats.syntax.order._

    val comparison = LocalDate.now.plusDays(1) compare LocalDate.now()
    comparison shouldEqual 1
  }

  it should "encode an Activity as a URL" in {
    forAll(Gen.oneOf(Activity.values)) { a =>
      val expected = a.toString.replaceAll("(^[A-Z].*)([A-Z])", "$1-$2").toLowerCase
      Activity.toUrl(a) shouldEqual expected
    }
  }

  it should "combine and empty Money correctly" in {
    import Money.mon
    forAll { (a: Money, b: Money) =>
      a.combine(b) - a shouldEqual b
      a.combine(b) - b shouldEqual a
      a.combine(b) - b - a shouldEqual mon.empty
    }
  }
}

