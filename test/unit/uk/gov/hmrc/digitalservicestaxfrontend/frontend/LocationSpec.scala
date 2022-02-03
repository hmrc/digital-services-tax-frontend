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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.frontend

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.digitalservicestax.frontend.Location

import scala.collection.immutable

class LocationSpec extends AnyFlatSpec with Matchers with MockitoSugar {

  "Location" should "filter out Alpha-3 codes. Accepting only Alpha-2 codes" in {
    val locations: immutable.Seq[Location.Location] = Location.locations
    locations.length shouldBe 275
    locations shouldNot contain(Location.Location("Akrotiri", "XQZ", "territory"))
  }

  it should "return a corresponding name to a country code" in {
    Location.name("AF") shouldEqual "Afghanistan"
  }

  it should "return 'unknown' for unknown country code" in {
    Location.name("XQZ") shouldEqual "unknown"
  }

}
