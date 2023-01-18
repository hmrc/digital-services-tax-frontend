/*
 * Copyright 2023 HM Revenue & Customs
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

import uk.gov.hmrc.digitalservicestax.controllers._

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class PackageUtilitySpec extends AnyFlatSpec with Matchers {

  it should "re-order a list of radio buttons" in {
    val list = List("None", "Some")
    val res  = list.orderYesNoRadio

    res shouldEqual list.reverse
  }

  it should "not re-order a list of radio buttons if the order is correct" in {
    val list = List("Some", "None")
    val res  = list.orderYesNoRadio

    res shouldEqual list
  }

  it should "automatically HTML escape a string" in {
    val str = "<div></div>"
    str.escapeHtml shouldEqual "&lt;div&gt;&lt;/div&gt;"
  }

  it should "return the same list with orderYesNoRadio is the list length is different than two" in {
    val list = List("None", "Some", "None", "Some")
    val res  = list.orderYesNoRadio

    res shouldEqual list
  }

  it should "have correct value for fieldNameForSome" in {
    fieldNameForSome shouldEqual "value"
  }

}
