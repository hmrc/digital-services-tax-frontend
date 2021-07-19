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
package controllers

import cats.implicits.catsKernelOrderingForOrder
import ltbs.uniform.{UniformMessages, _}
import ltbs.uniform.common.web._
import play.api.data.Form
import play.api.data.Forms._
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.modules.reactivemongo.ReactiveMongoApi
import play.twirl.api.Html
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestax.connectors.{DSTConnector, MongoPersistence}
import uk.gov.hmrc.digitalservicestax.data.Period.Key
import uk.gov.hmrc.digitalservicestax.data._
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}


class ReturnsController @Inject()(
  authorisedAction: AuthorisedAction,
  http: HttpClient,
  servicesConfig: ServicesConfig,
  mongo: ReactiveMongoApi,
  interpreter: DSTInterpreter,
  val authConnector: AuthConnector,
  val messagesApi: MessagesApi
)(implicit
  ec: ExecutionContext
) extends ControllerHelpers
    with I18nSupport
    with AuthorisedFunctions
    with FrontendHeaderCarrierProvider
{

  import interpreter._
  private def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  // private implicit def autoGroupListingTell = new ListingTell[Html, GroupCompany] {
  //   def apply(rows: List[ListingTellRow[GroupCompany]], messages: UniformMessages[Html]): Html =
  //     views.html.uniform.listing(rows.map {
  //       case ListingTellRow(value, editLink, deleteLink) => (HtmlFormat.escape(value.name), editLink, deleteLink)
  //     }, messages)
  // }

  private implicit val cyaRetTell = new WebTell[Html, CYA[(Return, Period, CompanyName)]] {
    override def render(in: CYA[(Return, Period, CompanyName)], key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(views.html.cya.check_your_return_answers(s"$key.ret", in.value._1, in.value._2, in.value._3)(messages))
  }

  private implicit val confirmRetTell = new WebTell[Html, Confirmation[(Return, CompanyName, Period, Period, Option[Html])]] {
    override def render(in: Confirmation[(Return, CompanyName, Period, Period, Option[Html])], key: String, messages: UniformMessages[Html]): Option[Html] =
      Some(
        views.html.end.confirmation_return(key: String, in.value._2: CompanyName, in.value._3: Period, in.value._4: Period, in.value._5: Option[Html])(messages))
  }

  private def applyKey(key: Key): Period.Key = key
  private def unapplyKey(arg: Period.Key): Option[(Key)] = Option(arg)

  private val periodForm: Form[Period.Key] = Form(
    mapping(
      "key" -> nonEmptyText.transform(Period.Key.apply, {x: Period.Key => x.toString})
    )(applyKey)(unapplyKey)
  )

  def showAmendments(): Action[AnyContent] = authorisedAction.async {
    implicit request: AuthorisedRequest[AnyContent] =>
      implicit val msg: UniformMessages[Html] = messages(request)

      //TODO Check print feature from returns

       implicit val persistence: MongoPersistence[AuthorisedRequest[AnyContent]] =
         MongoPersistence[AuthorisedRequest[AnyContent]](
           mongo,
           collectionName = "uf-amendments-returns",
           appConfig.mongoJourneyStoreExpireAfter
         )(_.internalId)

      backend.lookupRegistration().flatMap{
        case None      => Future.successful(NotFound)
        case Some(_) => Future.successful(NotFound)
          backend.lookupAmendableReturns().map { outstandingPeriods =>
             outstandingPeriods.toList match {
               case Nil =>
                NotFound
               case periods =>
                Ok(views.html.main_template(
                    title =
                      s"${msg("resubmit-a-return.title")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
                )(views.html.resubmit_a_return("resubmit-a-return", periods, periodForm)(msg, request)))
             }
            }
          }
      }

  def postAmendments(): Action[AnyContent] = authorisedAction.async {
    implicit request: AuthorisedRequest[AnyContent] =>
    implicit val msg: UniformMessages[Html] = messages(request)
      backend.lookupAmendableReturns().flatMap { outstandingPeriods =>
        outstandingPeriods.toList match {
          case Nil =>
            Future.successful(NotFound)
          case periods =>
            periodForm.bindFromRequest.fold(
              formWithErrors => {
                Future.successful(
                  BadRequest(views.html.main_template(
                  title =
                    s"${msg("resubmit-a-return.title")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
                )(views.html.resubmit_a_return("resubmit-a-return", periods, formWithErrors)(msg, request))
                  )
                )
              },
              postedForm => {
                Future.successful(
                  Redirect(routes.ReturnsController.returnAction(postedForm, " "))
                )
              }
            )
        }
      }

  }

  implicit val persistence: MongoPersistence[AuthorisedRequest[AnyContent]] =
    MongoPersistence[AuthorisedRequest[AnyContent]](
      mongo,
      collectionName = "uf-returns",
      appConfig.mongoJourneyStoreExpireAfter
    )(_.internalId)

  def returnAction(periodKeyString: String, targetId: String = ""): Action[AnyContent] = authorisedAction.async {
    implicit request: AuthorisedRequest[AnyContent] =>
      implicit val msg: UniformMessages[Html] = interpreter.messages(request)
    import journeys.ReturnJourney._

    val periodKey = Period.Key(periodKeyString)

    backend.lookupRegistration().flatMap{
      case None      => Future.successful(NotFound)
      case Some(reg) =>
        backend.lookupAllReturns().flatMap { periods =>
          periods.find(_.key == periodKey) match {
            case None => Future.successful(NotFound)
            case Some(period) =>
              interpret(returnJourney(period, reg)).run(targetId){ ret =>
              val purgeStateUponCompletion = true

                backend.submitReturn(period, ret).map{ _ =>
                  persistence.cacheReturn(ret, purgeStateUponCompletion)
                  Redirect(routes.ReturnsController.returnComplete(periodKeyString))
                }
              }
          }
        }
    } 
  }

  def returnComplete(submittedPeriodKeyString: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] = messages(request)
    val submittedPeriodKey = Period.Key(submittedPeriodKeyString)
    for {
      ret <- persistence.retrieveCachedReturn
      reg <- backend.lookupRegistration()
      outstandingPeriods <- backend.lookupOutstandingReturns()
      allReturns <- backend.lookupAllReturns()
    } yield {
      allReturns.find(_.key == submittedPeriodKey) match {
        case None => NotFound
        case Some(period) =>
          val companyName = reg.fold(CompanyName(""))(_.companyReg.company.name)
          val printableCYA: Option[Html] = ret.map { r => views.html.cya.check_your_return_answers(
            "check-your-answers.ret", r, period, companyName, isPrint = true)(msg)}
          Ok(
            views.html.main_template(
            title = s"${msg("confirmation.heading")} - ${msg("common.title")} - ${msg("common.title.suffix")}"
            )(
              views.html.end.confirmation_return(
                "confirmation",
                companyName,
                period,
                outstandingPeriods.toList.minBy(_.start),
                printableCYA
              )(msg)
            )
          )
      }
    }
  }


}
