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

package uk.gov.hmrc.digitalservicestaxfrontend.controller_test

import cats.data.NonEmptyList
import ltbs.uniform._
import uk.gov.hmrc.digitalservicestaxfrontend.util.FakeApplicationSpec
import cats.implicits._
import ltbs.uniform.common.web.WebAsk
import org.scalatest.Assertion
import play.api.test.Helpers._
import play.twirl.api.Html
import uk.gov.hmrc.digitalservicestax.data.Activity
import uk.gov.hmrc.digitalservicestaxfrontend.TestInstances

import java.time.LocalDate
import scala.collection.immutable.ListMap
import scala.collection.mutable.WrappedArray

class DSTInterpreterSpec extends FakeApplicationSpec {

  "DSTInterpreter codecs" must {
    "decode & encode input" in {
      testCodecRoundtrip(true, interpreter.twirlBoolField)
      testCodecRoundtrip(false, interpreter.twirlBoolField)
      testCodecRoundtrip("blah", interpreter.twirlStringFields())
      testCodecRoundtrip(LocalDate.now, interpreter.twirlDateField)
      testCodecRoundtrip(TestInstances.arbForeignAddress.arbitrary.sample.get, interpreter.twirlForeignAddressField)
      testCodecRoundtrip(TestInstances.arbUkAddress.arbitrary.sample.get, interpreter.twirlUKAddressField)
      testCodecRoundtrip[Activity](Activity.SocialMedia, interpreter.enumeratumField)
    }
    "handle invalid boolean input" in {
      val fakeInput: Input = Map(Nil -> List("bar"))
      val Left(foobar) = interpreter.twirlBoolField.decode(fakeInput)
      val NonEmptyList(err, _) = foobar.values.toList.head
      err.msg mustBe "invalid"
    }
    "render radios for booleans" in {
      val Some(html) = interpreter.twirlBoolField.render(
        Nil,
        List("foo"),
        None,
        Nil,
        Input.empty,
        ErrorTree.empty,
        UniformMessages.echo.map(Html.apply)
      )
      contentAsString(html) must include("""type="radio" value="FALSE"""")
      contentAsString(html) must include("""type="radio" value="TRUE"""")
    }
  }

  def testCodecRoundtrip[A](raw: A, ask: WebAsk[Html, A]): Assertion = {
    val a = ask.encode(raw)
    val b = ask.decode(a)
    b mustBe Right(raw)
  }
}
