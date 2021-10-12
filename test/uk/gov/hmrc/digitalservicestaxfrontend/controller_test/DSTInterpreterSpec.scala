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
import play.twirl.api.Html

import scala.collection.immutable.ListMap
import scala.collection.mutable.WrappedArray

class DSTInterpreterSpec extends FakeApplicationSpec {

  "DSTInterpreter twirlBoolField" must {
    "decode & encode boolean" in {
      val bool = true
      val foo: Input = interpreter.twirlBoolField.encode(bool)
      val bar: Either[ErrorTree, Boolean] = interpreter.twirlBoolField.decode(foo)
      val fakeInput: Input = Map(Nil -> List("bar"))
      val foobar = interpreter.twirlBoolField.decode(fakeInput)
      println(foobar)
      bar mustBe Right(bool)


    }
//    "render radios" in {
//      val Some(html) = interpreter.twirlBoolField.render(
//        Nil,
//        Nil,
//        None,
//        Nil,
//        Input.empty,
//        ErrorTree.empty,
//        UniformMessages.echo.map(Html.apply)
//      )
//
//    }
  }
}
