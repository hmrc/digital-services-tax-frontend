/*
 * Copyright 2024 HM Revenue & Customs
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

import connectors.DSTService
import data._
import frontend.Kickout
import cats.Monad
import cats.implicits._
import java.time.LocalDate

import uk.gov.hmrc.digitalservicestax.frontend._
import ltbs.uniform._
import ltbs.uniform.validation._
import ltbs.uniform.validation.Rule._
import izumi.reflect.TagK

object RegJourney {

  private def message(key: String, args: String*) = {
    import play.twirl.api.HtmlFormat.escape
    Map(key -> Tuple2(key, args.toList.map(escape(_).toString)))
  }

  def nameCheck(name: CompanyName): Boolean = name.matches("""^[a-zA-Z0-9 '&-]{1,105}$""")

  def registrationJourney[F[_]: Monad: TagK](
    backendService: DSTService[F]
  ) = {

    for {
      _                 <- end("global-revenues-not-eligible", Kickout("global-revenues-not-eligible")) unless ask[Boolean](
                             "global-revenues"
                           )
      _                 <- end("uk-revenues-not-eligible", Kickout("uk-revenues-not-eligible")) unless ask[Boolean]("uk-revenues")
      companyRegWrapper <-
        convertWithKey("lookup-company")(backendService.lookupCompany()) flatMap { // gets a CompanyRegWrapper but converts to a company

          // found a matching company
          case Some(companyRW) =>
            for {
              _ <- end("details-not-correct", Kickout("details-not-correct")) unless interact[Boolean](
                     "confirm-company-details",
                     companyRW.company
                   )
            } yield companyRW // useSafeId is false, no utr or safeId sent

          // no matching company found
          case None            =>
            ask[Boolean]("check-company-registered-office-address") flatMap {
              case false =>
                for {
                  companyName    <- ask[CompanyName](
                                      "company-name",
                                      validation = cond[CompanyName](nameCheck, "invalid")
                                    )
                  companyAddress <- ask[ForeignAddress](
                                      "company-registered-office-international-address",
                                      customContent = message(
                                        "company-registered-office-international-address.listing.heading",
                                        companyName
                                      )
                                    ).map(identity)
                } yield CompanyRegWrapper(Company(companyName, companyAddress), useSafeId = true)

              case true =>
                for {
                  postcode       <- ask[Postcode]("company-registered-office-postcode")
                  companyWrapper <- ask[Boolean]("check-unique-taxpayer-reference") flatMap {
                                      case false =>
                                        for {
                                          companyName    <- ask[CompanyName](
                                                              "company-name",
                                                              validation = cond[CompanyName](nameCheck, "invalid")
                                                            )
                                          companyAddress <- ask[UkAddress](
                                                              "company-registered-office-uk-address",
                                                              customContent = message(
                                                                "company-registered-office-uk-address.listing.heading",
                                                                companyName
                                                              )
                                                            ).map(identity)
                                        } yield CompanyRegWrapper(
                                          Company(companyName, companyAddress),
                                          useSafeId = true
                                        )

                                      case true =>
                                        for {
                                          utr        <- ask[UTR]("enter-utr")
                                          companyOpt <- convert(backendService.lookupCompany(utr, postcode)) flatMap {
                                                          case None      =>
                                                            for {
                                                              companyName    <- ask[CompanyName](
                                                                                  "company-name",
                                                                                  validation =
                                                                                    cond[CompanyName](nameCheck, "invalid")
                                                                                )
                                                              companyAddress <-
                                                                ask[UkAddress](
                                                                  "company-registered-office-uk-address",
                                                                  customContent = message(
                                                                    "company-registered-office-uk-address.listing.heading",
                                                                    companyName
                                                                  )
                                                                ).map(identity)
                                                            } yield CompanyRegWrapper(
                                                              Company(companyName, companyAddress),
                                                              useSafeId = true
                                                            )
                                                          case Some(crw) =>
                                                            for {
                                                              confirmCompany <- interact[Boolean](
                                                                                  "confirm-company-details",
                                                                                  crw.company
                                                                                )
                                                              _              <- if (!confirmCompany) {
                                                                                  end(
                                                                                    "details-not-correct",
                                                                                    Kickout("details-not-correct")
                                                                                  )
                                                                                } else {
                                                                                  pure(())
                                                                                }
                                                            } yield CompanyRegWrapper(crw.company, utr.some, crw.safeId)
                                                        }
                                        } yield companyOpt
                                    }
                } yield companyWrapper
            }
        }

      registration <- {
        for {
          companyRegWrapper <- pure(companyRegWrapper)
          contactAddress    <- (
                                 ask[Boolean]("check-contact-address") flatMap {
                                   case true  =>
                                     ask[UkAddress](
                                       "contact-uk-address"
                                     ).map(identity)
                                   case false =>
                                     ask[ForeignAddress](
                                       "contact-international-address"
                                     ).map(identity)
                                 }
                               ) unless interact[Boolean]("company-contact-address", companyRegWrapper.company.address)
          isGroup           <- ask[Boolean]("check-if-group")
          ultimateParent    <- if (isGroup) {
                                 for {
                                   parentName    <- ask[CompanyName]("ultimate-parent-company-name")
                                   parentAddress <-
                                     ask[Boolean](
                                       "check-ultimate-parent-company-address",
                                       customContent =
                                         message("check-ultimate-parent-company-address.heading", parentName) ++
                                           message("check-ultimate-parent-company-address.required", parentName)
                                     ) flatMap {
                                       case true  =>
                                         ask[UkAddress](
                                           "ultimate-parent-company-uk-address",
                                           customContent =
                                             message("ultimate-parent-company-uk-address.listing.heading", parentName)
                                         ).map(identity)
                                       case false =>
                                         ask[ForeignAddress](
                                           "ultimate-parent-company-international-address",
                                           customContent = message(
                                             "ultimate-parent-company-international-address.listing.heading",
                                             parentName
                                           )
                                         ).map(identity)
                                     }
                                 } yield Company(parentName, parentAddress).some
                               } else {
                                 pure(Option.empty[Company])
                               }
          contactDetails    <- ask[ContactDetails]("contact-details")
          groupMessage       = if (isGroup) "group" else "company"
          liabilityDate     <- ask[LocalDate](
                                 "liability-start-date",
                                 validation = Rule.min(LocalDate.of(2020, 4, 1), "minimum-date") followedBy
                                   Rule.max(LocalDate.now.plusYears(1), "maximum-date"),
                                 customContent = message(
                                   "liability-start-date.maximum-date",
                                   groupMessage,
                                   formatDate(LocalDate.now.plusYears(1))
                                 ) ++
                                   message("liability-start-date.minimum-date", groupMessage) ++
                                   message("liability-start-date.day-and-month-and-year.empty", groupMessage) ++
                                   message("liability-start-date.day.empty", groupMessage) ++
                                   message("liability-start-date.month.empty", groupMessage) ++
                                   message("liability-start-date.year.empty", groupMessage) ++
                                   message("liability-start-date.day-and-month.empty", groupMessage) ++
                                   message("liability-start-date.day-and-year.empty", groupMessage) ++
                                   message("liability-start-date.month-and-year.empty", groupMessage) ++
                                   message("liability-start-date.day-and-month-and-year.nan", groupMessage) ++
                                   message("liability-start-date.day-and-month-and-year.invalid", groupMessage) ++
                                   message("liability-start-date.not-a-date", groupMessage)
                               )
          periodEndDate     <- ask[LocalDate](
                                 "accounting-period-end-date",
                                 validation = liabilityDate match {
                                   case ld if ld == LocalDate.of(2020, 4, 1) =>
                                     Rule.min(LocalDate.of(2020, 4, 2), "minimum-date") followedBy
                                       Rule.max(liabilityDate.plusYears(1).minusDays(1), "fixed-maximum-date")
                                   case _                                    =>
                                     Rule.min(LocalDate.of(2020, 4, 2), "minimum-date") followedBy
                                       Rule.max(liabilityDate.plusYears(1).minusDays(1), "maximum-date")
                                 },
                                 customContent = message(
                                   "accounting-period-end-date.maximum-date",
                                   groupMessage,
                                   formatDate(liabilityDate.plusYears(1))
                                 ) ++
                                   message(
                                     "accounting-period-end-date.fixed-maximum-date",
                                     groupMessage,
                                     formatDate(liabilityDate.plusYears(1).minusDays(1))
                                   ) ++
                                   message("accounting-period-end-date.minimum-date", groupMessage) ++
                                   message("accounting-period-end-date.day-and-month-and-year.empty", groupMessage) ++
                                   message("accounting-period-end-date.day.empty", groupMessage) ++
                                   message("accounting-period-end-date.month.empty", groupMessage) ++
                                   message("accounting-period-end-date.year.empty", groupMessage) ++
                                   message("accounting-period-end-date.day-and-month.empty", groupMessage) ++
                                   message("accounting-period-end-date.day-and-year.empty", groupMessage) ++
                                   message("accounting-period-end-date.month-and-year.empty", groupMessage) ++
                                   message("accounting-period-end-date.day-and-month-and-year.nan", groupMessage) ++
                                   message("accounting-period-end-date.day-and-month-and-year.invalid", groupMessage) ++
                                   message("accounting-period-end-date.not-a-date", groupMessage)
                               )
          emptyRegNo        <- pure(None)
        } yield Registration(
          companyRegWrapper,
          contactAddress,
          ultimateParent,
          contactDetails,
          liabilityDate,
          periodEndDate,
          emptyRegNo
        )
      }

      _ <- tell("check-your-answers", CYA(registration))
    } yield registration
  }

}
