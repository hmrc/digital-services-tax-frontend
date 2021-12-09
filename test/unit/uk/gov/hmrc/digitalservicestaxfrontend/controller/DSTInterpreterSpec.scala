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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.controller

import cats.data.NonEmptyList
import ltbs.uniform._
import ltbs.uniform.common.web.{WebAsk, WebTell}
import org.scalatest.Assertion
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestax.frontend.Kickout
import unit.uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationServer
import unit.uk.gov.hmrc.digitalservicestaxfrontend.{ConfiguredPropertyChecks, TestInstances}
import uk.gov.hmrc.govukfrontend.views.Implicits.RichHtml

import java.time.LocalDate

class DSTInterpreterSpec extends FakeApplicationServer with ConfiguredPropertyChecks {

  import TestInstances._
  "DSTInterpreter codec " must {
    "have round trip parity " in {
      forAll {(ld: LocalDate, fa: ForeignAddress, uka: UkAddress, a: Activity, s: String, b: Boolean) =>
        testCodecRoundtrip(ld, interpreter.twirlDateField)
        testCodecRoundtrip(fa, interpreter.twirlForeignAddressField)
        testCodecRoundtrip(uka, interpreter.twirlUKAddressField)
        testCodecRoundtrip[Activity](a, interpreter.enumeratumField)
        testCodecRoundtrip(s, interpreter.twirlStringFields())
        testCodecRoundtrip(b, interpreter.twirlBoolField)
      }
      forAll {(cc: CountryCode, as: Set[Activity], u: Unit, i: Int, oUtr: Option[UTR], an: AccountName) =>
        testCodecRoundtrip(cc, interpreter.twirlCountryCodeField)
        testCodecRoundtrip(u, interpreter.askUnit)
        testCodecRoundtrip[Set[Activity]](as, interpreter.enumeratumSetField)
        testCodecRoundtrip(i, interpreter.intField)
        testCodecRoundtrip(oUtr, interpreter.optUtrField)
        testCodecRoundtrip(an, interpreter.accountNameField)

      }
      forAll { (anf: AccountNumber, bd: BigDecimal, bsri: Option[BuildingSocietyRollNumber], iban: IBAN, sc: SortCode, m: Money) =>
        testCodecRoundtrip(anf, interpreter.accountNumberField)
        testCodecRoundtrip(bd, interpreter.bigdecimalField)
        testCodecRoundtrip(bsri, interpreter.BuildingSocietyRollNumberField)
        testCodecRoundtrip(iban, interpreter.ibanField)
        testCodecRoundtrip(sc, interpreter.sortCodeField)
        testCodecRoundtrip(m, interpreter.moneyField)
      }
      forAll { (p: Percent, f: Float, l: Long, nes: NonEmptyString) =>
        testCodecRoundtrip(p, interpreter.percentField)
        testCodecRoundtrip(f, interpreter.floatField)
        testCodecRoundtrip(l, interpreter.longField)
        testCodecRoundtrip(nes, interpreter.nesField)
      }
    }

  }

  "DSTInterpreter render" must {
    "return Html" in {
      forAll { (ld: LocalDate, fa: ForeignAddress, uka: UkAddress, a: Activity, s: String, b: Boolean) =>
        testRender(ld, interpreter.twirlDateField)
        testRender(fa, interpreter.twirlForeignAddressField)
        testRender(uka, interpreter.twirlUKAddressField)
        testRender[Activity](a, interpreter.enumeratumField, containsRadio)
        testRender(s, interpreter.twirlStringFields())
        testRender(b, interpreter.twirlBoolField, containsRadio)
        testRender(b, interpreter.twirlBoolField, containsRadio, List("about-you"))
      }
      forAll { (cc: CountryCode, as: Set[Activity], u: Unit, i: Int, oUtr: Option[UTR], an: AccountName) =>
        testRender(cc, interpreter.twirlCountryCodeField)
        testRender[Set[Activity]](as, interpreter.enumeratumSetField, html => contentAsString(html) must include("""type="checkbox""""))
        testRender(u, interpreter.askUnit)
        testRender(i, interpreter.intField)
        testRender(oUtr, interpreter.optUtrField)
        testRender(an, interpreter.accountNameField)
      }
      forAll { (anf: AccountNumber, bd: BigDecimal, bsri: Option[BuildingSocietyRollNumber], iban: IBAN, sc: SortCode, m: Money) =>
        testRender(anf, interpreter.accountNumberField)
        testRender(bd, interpreter.bigdecimalField)
        testRender(bsri, interpreter.BuildingSocietyRollNumberField)
        testRender(iban, interpreter.ibanField)
        testRender(sc, interpreter.sortCodeField)
        testRender(m, interpreter.moneyField)
      }
      forAll { (p: Percent, f: Float, l: Long, nes: NonEmptyString) =>
        testRender(p, interpreter.percentField)
        testRender(f, interpreter.floatField)
        testRender(l, interpreter.longField)
        testRender(nes, interpreter.nesField)
      }
    }
  }

  "DSTInterpreter codecs" must {
    "handle invalid input" in {
        testErrors(interpreter.twirlBoolField)
        testErrors[Activity](interpreter.enumeratumField)
        testErrors[Set[Activity]](interpreter.enumeratumSetField)
        testErrors(interpreter.twirlDateField, "day-and-month-and-year.empty")
        testErrors(interpreter.twirlDateField, "not-a-date", Map(List("year") -> List("2017"), List("month") -> List("2"), List("day") -> List("30")))
        testErrors(interpreter.nesField,"invalid", Map(Nil -> List("")))
        testErrors(interpreter.BuildingSocietyRollNumberField, "length.exceeded", Map(Nil -> List("asdfasdfasdfasdfasdf")))
        testErrors(interpreter.BuildingSocietyRollNumberField, "invalid", Map(Nil -> List("!!!")))
        testErrors(interpreter.bigdecimalField, "not-a-number")
    }
  }

  "DSTInterpreter " must {
    "return Html for renderAnd with valid members" in {
      val foo = interpreter.renderAnd(
        Nil,
        List("foo"),
        None,
        Nil,
        Input.empty,
        ErrorTree.empty,
        UniformMessages.echo.map(Html.apply),
        List(("foo", Html("memberA")), ("bar", Html("memberB")))
      )
      foo.nonEmpty mustBe true
    }
    "return Html with radios for renderOr with valid alternatives" in {
      val foo = interpreter.renderOr(
        Nil,
        List("foo"),
        None,
        Nil,
        Input.empty,
        ErrorTree.empty,
        UniformMessages.echo.map(Html.apply),
        List(("foo", None), ("bar", None)),
        None
      )
      foo.nonEmpty mustBe true
      containsRadio(foo)
    }
    "return Html when 'telling'" in {
      forAll { (c: Company, b: Boolean, k: Kickout, a: Address) =>
        testRenderTell(interpreter.companyTell, c)
        testRenderTell(interpreter.booleanTell, b)
        testRenderTell(interpreter.kickoutTell, k)
        testRenderTell(interpreter.addressTell, a)
      }
    }
  }

  def testErrors[A](
        ask: WebAsk[Html, A],
        expectedErrMsg: String = "invalid",
        badInput: Input = Map(Nil -> List("bar")),
  ): Assertion = {
    val Left(foo) = ask.decode(badInput)
    val NonEmptyList(err, _) = foo.values.toList.head
    err.msg mustBe expectedErrMsg
  }

  def testRenderTell[A](
    tell: WebTell[Html, A],
    in: A,
    key: String = "foo",
    extraAssertion: Html =>  Assertion = _ => 1 mustBe 1,
    msg: UniformMessages[Html] = UniformMessages.echo.map(Html.apply)
  ): Assertion = {
    val foo = tell.render(in, key, msg)
    foo.nonEmpty mustBe true
    extraAssertion(foo.get)
  }

  def testRender[A](
    raw: A,
    ask: WebAsk[Html, A],
    extraAssertion: Html =>  Assertion = _ => 1 mustBe 1,
    pageKey: List[String] = Nil
  ): Assertion = {
    val foo = ask.render(
      pageKey,
      List("foo"),
      None,
      Nil,
      ask.encode(raw),
      ErrorTree.empty,
      UniformMessages.echo.map(Html.apply)
    )
    foo.nonEmpty mustBe true
    extraAssertion(foo.get)
  }

  def containsRadio(html: Html): Assertion =
    contentAsString(html) must include("""type="radio"""")

  def testCodecRoundtrip[A](raw: A, ask: WebAsk[Html, A]): Assertion = {
    val a = ask.encode(raw)
    val b = ask.decode(a)
    b mustBe Right(raw)
  }
}
