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

package uk.gov.hmrc.digitalservicestax
package journeys

import data._
import frontend.formatDate
import cats.implicits._
import ltbs.uniform._
import ltbs.uniform.validation._
import uk.gov.hmrc.digitalservicestax.data.Activity.OnlineMarketplace

object ReturnJourney {

  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map { escape(_).toString } ))
  }

  def returnJourney(
    period: Period,
    registration: Registration
  ) = {

    val isGroup = registration.ultimateParent.isDefined
    val isGroupMessage = if(isGroup) "group" else "company"

    def askAlternativeCharge(applicableActivities: Set[Activity]) = {

      def askActivityReduced(actType: Activity) =
        { ask[Percent](
          s"report-${Activity.toUrl(actType)}-operating-margin",
          customContent =
            message(s"report-${Activity.toUrl(actType)}-operating-margin.heading", isGroupMessage) ++
            message(s"report-${Activity.toUrl(actType)}-operating-margin.required", isGroupMessage) ++
            message(s"report-${Activity.toUrl(actType)}-operating-margin.not-a-number", isGroupMessage) ++
            message(s"report-${Activity.toUrl(actType)}-operating-margin.invalid", isGroupMessage)
        ) emptyUnless ask[Boolean](
            s"report-${Activity.toUrl(actType)}-loss",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-loss.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-loss.required", isGroupMessage)
          ).map { x => !x }}

      def askActivity(actType: Activity) = 
        { ask[Percent](
            s"report-${Activity.toUrl(actType)}-operating-margin",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-operating-margin.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-operating-margin.required", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-operating-margin.not-a-number", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-operating-margin.invalid", isGroupMessage)
        ) emptyUnless ask[Boolean](
            s"report-${Activity.toUrl(actType)}-loss",
            customContent =
              message(s"report-${Activity.toUrl(actType)}-loss.heading", isGroupMessage) ++
              message(s"report-${Activity.toUrl(actType)}-loss.required", isGroupMessage)
        ).map { x => !x }} when ask[Boolean](s"report-${Activity.toUrl(actType)}-alternative-charge")
      
      val allEntries =
        if(applicableActivities.size == 1) {
          applicableActivities.toList.map { actType =>
            askActivityReduced(actType).map { percent =>
              (actType, percent.some)
            }
          }
        } else {
          applicableActivities.toList.map { actType =>
            askActivity(actType).map {
              (actType, _)
            }
          }
        }

      implicit val gApp: cats.Applicative[Uniform[Needs.Ask[Percent] with Needs.Ask[Boolean], ?, Unit]] = Uniform.uniformMonadInstance

      val step1 = allEntries.sequence[Uniform[Needs.Ask[Percent] with Needs.Ask[Boolean], ?, Unit], (Activity, Option[Percent])]

      step1.map { _.collect {
        case (a, Some(x)) => a -> x
      }.toMap }
    } emptyUnless ask[Boolean]("report-alternative-charge")

    def askAmountForCompanies(companies: Option[List[GroupCompany]]) = {
      companies.fold(pure(Map.empty[GroupCompany, Money]): Uniform[ltbs.uniform.Needs.Ask[Money] with Needs.Tell[GroupCompany],Map[GroupCompany,Money],GroupCompany]){x =>

        val z = x.zipWithIndex

        val w: List[Uniform[Needs.Ask[Money] with Needs.Tell[GroupCompany],(GroupCompany, Money),GroupCompany]] = z.map{ case (co, i) =>
        interact[Money](
          s"company-liabilities-$i",
          co,
          customContent =
            message(s"company-liabilities-$i.heading", co.name, formatDate(period.start), formatDate(period.end)) ++
              message(s"company-liabilities-$i.required", co.name) ++
              message(s"company-liabilities-$i.not-a-number", co.name) ++
              message(s"company-liabilities-$i.length.exceeded", co.name) ++
              message(s"company-liabilities-$i.invalid", co.name)
        ).map{(co, _)}
        }

        w.sequence[Uniform[Needs.Ask[Money] with Needs.Tell[GroupCompany],?,GroupCompany],(GroupCompany, Money)].map{_.toMap}}
    }

    for {
      groupCos <- ask[List[GroupCompany]]("manage-companies", validation = Rule.minLength(1)) when isGroup
      activities <- ask[Set[Activity]]("select-activities", validation = Rule.minLength(1))

      dstReturn <- for {
        a <- askAlternativeCharge(activities)
        b <- ask[Boolean]("report-cross-border-transaction-relief") when activities.contains(OnlineMarketplace) flatMap {
          case Some(true) => ask[Money]("relief-deducted")
          case _ => pure(Money(BigDecimal(0).setScale(2)))
        }
        c <- askAmountForCompanies(groupCos) emptyUnless isGroup
        d <- ask[Money](
          "allowance-deducted",
          validation =
            Rule.cond[Money]({
              case money: Money if money <= 25000000 => true
              case _ => false
            }, "max-money")
        )
        e <- ask[Money]("group-liability",
          customContent =
            message("group-liability.heading", isGroupMessage, formatDate(period.start), formatDate(period.end)) ++
            message("group-liability.required", isGroupMessage, formatDate(period.start), formatDate(period.end)) ++
            message("group-liability.not-a-number", isGroupMessage) ++
            message("group-liability.length.exceeded", isGroupMessage) ++
            message("group-liability.invalid", isGroupMessage)
        )
        f <- ask[RepaymentDetails]("bank-details") when ask[Boolean](
            "repayment",
            customContent =
            message("repayment.heading", formatDate(period.start), formatDate(period.end)) ++
            message("repayment.required", formatDate(period.start), formatDate(period.end))
        )
      } yield Return(a,b,c,d,e,f)
      displayName = registration.ultimateParent.fold(registration.companyReg.company.name)(_.name)
      _ <- tell("check-your-answers", CYA((dstReturn, period, displayName)))
    } yield dstReturn
  }

}
