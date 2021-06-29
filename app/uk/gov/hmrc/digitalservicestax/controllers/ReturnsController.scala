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

import cats.implicits.catsStdBimonadForFunction0.<*
import data._
import data.BackendAndFrontendJson._
import config.AppConfig
import connectors.{DSTConnector, MongoPersistence}
import cats.implicits.{catsKernelOrderingForOrder, catsSyntaxFlatMapOps}

import javax.inject.Inject
import ltbs.uniform._
import common.web._
import interpreters.playframework._
import WebMonad.webMonadMonadInstance
import cats.Functor.ops.toAllFunctorOps
import com.fasterxml.jackson.core.JsonParseException
import play.api.i18n.{I18nSupport, MessagesApi}
import play.api.mvc.{Action, AnyContent, ControllerHelpers}
import play.modules.reactivemongo.ReactiveMongoApi
import play.twirl.api.{Html, HtmlFormat}

import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.auth.core.{AuthConnector, AuthorisedFunctions}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.{AuthorisedAction, AuthorisedRequest}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.controller.FrontendHeaderCarrierProvider
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.Json
import uk.gov.hmrc.digitalservicestax.data.Period.Key

class ReturnsController @Inject()(
  authorisedAction: AuthorisedAction,
  http: HttpClient,
  servicesConfig: ServicesConfig,
  mongo: ReactiveMongoApi,
  val authConnector: AuthConnector,
  val messagesApi: MessagesApi  
)(implicit
  appConfig: AppConfig,
  ec: ExecutionContext
) extends ControllerHelpers
    with I18nSupport
    with AuthorisedFunctions
    with FrontendHeaderCarrierProvider
{

  val interpreter = DSTInterpreter(appConfig, this, messagesApi)
  private def backend(implicit hc: HeaderCarrier) = new DSTConnector(http, servicesConfig)

  private implicit def autoGroupListingTell = new ListingTell[Html, GroupCompany] {
    def apply(rows: List[ListingTellRow[GroupCompany]], messages: UniformMessages[Html]): Html =
      views.html.uniform.listing(rows.map {
        case ListingTellRow(value, editLink, deleteLink) => (HtmlFormat.escape(value.name), editLink, deleteLink)
      }, messages)
  }

  private implicit val cyaRetTell = new GenericWebTell[CYA[(Return, Period, CompanyName)], Html] {
    override def render(in: CYA[(Return, Period, CompanyName)], key: String, messages: UniformMessages[Html]): Html =
      views.html.cya.check_your_return_answers(s"$key.ret", in.value._1, in.value._2, in.value._3)(messages)
  }

  private implicit val confirmRetTell = new GenericWebTell[Confirmation[(Return, CompanyName, Period, Period, Html)], Html] {
    override def render(in: Confirmation[(Return, CompanyName, Period, Period, Html)], key: String, messages: UniformMessages[Html]): Html =
      views.html.end.confirmation_return(key: String, in.value._2: CompanyName, in.value._3: Period, in.value._4: Period, in.value._5: Html)(messages)
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
      implicit val msg: UniformMessages[Html] = interpreter.messages(request)

      implicit val persistence: PersistenceEngine[AuthorisedRequest[AnyContent]] =
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
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)
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
    import interpreter.{appConfig => _, _}
    import journeys.ReturnJourney._

    val periodKey = Period.Key(periodKeyString)

    backend.lookupRegistration().flatMap{
      case None      => Future.successful(NotFound)
      case Some(reg) =>
        backend.lookupAllReturns().flatMap { periods =>
          periods.find(_.key == periodKey) match {
            case None => Future.successful(NotFound)
            case Some(period) =>
              val playProgram: WebMonad[Return, Html] = returnJourney[WM](
                create[ReturnTellTypes, ReturnAskTypes](messages(request)),
                period,
                reg
              ) >>= { ret => interpreter.db.update[String](List(s"return-$periodKeyString"), Json.toJson(ret).toString()).map(_=> ret)}
              playProgram.run(targetId, purgeStateUponCompletion = false, config = JourneyConfig(askFirstListItem = true)) { ret =>
                backend.submitReturn(period, ret).map{ _ =>
                  Redirect(routes.ReturnsController.returnComplete(periodKeyString))
                }
              }
          }
        }
    } 
  }

  def getReturn(submittedPeriodKeyString: String)(implicit request: AuthorisedRequest[AnyContent]): Future[Return] = {
    import interpreter.{appConfig => _, _}
    persistence.getDirect[String](s"return-$submittedPeriodKeyString").map {
      case Right(retString) =>
        Json.fromJson[Return](
          Json.parse(retString)
        ).getOrElse(throw new IllegalStateException("unable to parse result from mongo"))
      case _ => throw new IllegalStateException("no return found")
    }
  }

  def returnComplete(submittedPeriodKeyString: String): Action[AnyContent] = authorisedAction.async { implicit request =>
    implicit val msg: UniformMessages[Html] = interpreter.messages(request)
    val submittedPeriodKey = Period.Key(submittedPeriodKeyString)
    for {
      ret <- getReturn(submittedPeriodKeyString)
      reg <- backend.lookupRegistration()
      outstandingPeriods <- backend.lookupOutstandingReturns()
      allReturns <- backend.lookupAllReturns()
    } yield {
      allReturns.find(_.key == submittedPeriodKey) match {
        case None => NotFound
        case Some(period) =>
          val companyName = reg.fold(CompanyName(""))(_.companyReg.company.name)
          val printableCYA: Html = views.html.cya.check_your_return_answers(
            "check-your-answers.ret", ret, period, companyName)(msg)
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
