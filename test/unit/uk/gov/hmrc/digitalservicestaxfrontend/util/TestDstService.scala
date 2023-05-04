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

package unit.uk.gov.hmrc.digitalservicestaxfrontend.util

import cats.Id
import cats.implicits._
import ltbs.uniform.interpreters.logictable.Logic
import uk.gov.hmrc.digitalservicestax.connectors.DSTService
import unit.uk.gov.hmrc.digitalservicestaxfrontend.data.TestSampleData._
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.http.HttpResponse

trait TestDstService extends DSTService[Id] {
  def lookupCompany(): Option[CompanyRegWrapper]                             = sampleCompanyRegWrapper.some
  def lookupCompany(utr: UTR, postcode: Postcode): Option[CompanyRegWrapper] = utrLookupCompanyRegWrapper.some
  def submitRegistration(reg: Registration): HttpResponse                    = HttpResponse(200, "")
  def submitReturn(period: Period, ret: Return): HttpResponse                = HttpResponse(200, "")
  def lookupRegistration(): Option[Registration]                             = sampleReg.some
  def lookupPendingRegistrationExists(): Boolean                             = false
  def lookupOutstandingReturns(): Set[Period]                                = Set.empty
  def lookupAmendableReturns(): Set[Period]                                  = Set.empty
  def lookupAllReturns(): Set[Period]                                        = Set.empty
  def get: DSTService[Logic]                                                 = this.transform(ToLogic)
}
