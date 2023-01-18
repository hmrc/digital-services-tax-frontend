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

package uk.gov.hmrc.digitalservicestax.connectors

import cats.~>
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.http.HttpResponse

trait DSTService[F[_]] {

  def lookupCompany(): F[Option[CompanyRegWrapper]]

  def lookupCompany(utr: UTR, postcode: Postcode): F[Option[CompanyRegWrapper]]

  def submitRegistration(reg: Registration): F[HttpResponse]

  def submitReturn(period: Period, ret: Return): F[HttpResponse]

  def lookupRegistration(): F[Option[Registration]]

  def lookupOutstandingReturns(): F[Set[Period]]

  def lookupAmendableReturns(): F[Set[Period]]

  def lookupAllReturns(): F[Set[Period]]

  def transform[G[_]](nat: F ~> G): DSTService[G] = {
    val old = this
    new DSTService[G] {
      def lookupCompany(utr: UTR, postcode: Postcode): G[Option[CompanyRegWrapper]] = nat(
        old.lookupCompany(utr, postcode)
      )

      def lookupCompany(): G[Option[CompanyRegWrapper]] = nat(old.lookupCompany())

      def lookupOutstandingReturns(): G[Set[Period]] = nat(old.lookupOutstandingReturns())

      def lookupRegistration(): G[Option[Registration]] = nat(old.lookupRegistration())

      def submitRegistration(reg: Registration): G[HttpResponse] = nat(old.submitRegistration(reg))

      def submitReturn(period: Period, ret: Return): G[HttpResponse] = nat(old.submitReturn(period, ret))

      def lookupAmendableReturns(): G[Set[Period]] = nat(old.lookupAmendableReturns())

      def lookupAllReturns(): G[Set[Period]] = nat(old.lookupAllReturns())
    }
  }
}
