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

package uk.gov.hmrc.digitalservicestaxfrontend.util

import akka.actor.ActorSystem
import com.softwaremill.macwire.wire
import org.scalatest.TryValues
import org.scalatest.concurrent.ScalaFutures
import org.scalatestplus.play.{BaseOneAppPerSuite, FakeApplicationFactory, PlaySpec}
import play.api.i18n.{Lang, MessagesApi}
import play.api.inject.DefaultApplicationLifecycle
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.WSClient
import play.api.mvc.MessagesControllerComponents
import play.api.{Application, ApplicationLoader}
import play.modules.reactivemongo.DefaultReactiveMongoApi
import reactivemongo.api.MongoConnection
import uk.gov.hmrc.auth.core.PlayAuthConnector
import uk.gov.hmrc.digitalservicestax.config.AppConfig
import uk.gov.hmrc.digitalservicestax.connectors.DSTConnector
import uk.gov.hmrc.digitalservicestax.controllers.DSTInterpreter
import uk.gov.hmrc.digitalservicestax.test.TestConnector
import uk.gov.hmrc.digitalservicestax.views.html.cya.{CheckYourAnswersReg, CheckYourAnswersRet}
import uk.gov.hmrc.digitalservicestax.views.html.end.{ConfirmationReg, ConfirmationReturn}
import uk.gov.hmrc.digitalservicestax.views.html.{Landing, Layout, PayYourDst}
import uk.gov.hmrc.digitalservicestaxfrontend.actions.AuthorisedAction
import uk.gov.hmrc.digitalservicestaxfrontend.controller.FakeAuthorisedAction
import uk.gov.hmrc.http.HttpClient
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.audit.http.HttpAuditing
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig
import uk.gov.hmrc.play.bootstrap.http.DefaultHttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

trait FakeApplicationSpec extends PlaySpec
  with BaseOneAppPerSuite
  with FakeApplicationFactory
  with TryValues
  with MongoSpecSupport
  with ScalaFutures
  with TestWiring {

  protected[this] val context: ApplicationLoader.Context = ApplicationLoader.Context.create(environment)

  implicit lazy val actorSystem: ActorSystem = app.actorSystem

  lazy val messagesApi: MessagesApi = app.injector.instanceOf[MessagesApi]
  lazy val wsClient: WSClient = app.injector.instanceOf[WSClient]
  lazy val httpAuditing: HttpAuditing = app.injector.instanceOf[HttpAuditing]
  lazy val httpClient: HttpClient = new DefaultHttpClient(configuration, httpAuditing, wsClient, actorSystem)
  lazy val authorisedAction: AuthorisedAction = app.injector.instanceOf[AuthorisedAction]
  lazy val authConnector: PlayAuthConnector = new DefaultAuthConnector(httpClient, servicesConfig)
  lazy val layoutInstance: Layout = app.injector.instanceOf[Layout]
  lazy val landingInstance: Landing = app.injector.instanceOf[Landing]
  lazy val checkYourAnswersRegInstance: CheckYourAnswersReg = app.injector.instanceOf[CheckYourAnswersReg]
  lazy val checkYourAnswersRetInstance: CheckYourAnswersRet = app.injector.instanceOf[CheckYourAnswersRet]
  lazy val confirmationRegInstance: ConfirmationReg  = app.injector.instanceOf[ConfirmationReg]
  lazy val confirmationRetInstance: ConfirmationReturn  = app.injector.instanceOf[ConfirmationReturn]
  lazy val payYourDst: PayYourDst = app.injector.instanceOf[PayYourDst]
  lazy val interpreter = app.injector.instanceOf[DSTInterpreter]

  lazy val appConfig: AppConfig = wire[AppConfig]
  val servicesConfig: ServicesConfig = wire[ServicesConfig]
  lazy val mockDSTConnector = mock[DSTConnector]

  val testConnector: TestConnector = new TestConnector(httpClient, servicesConfig)

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder(environment = environment).configure(
      Map(
        "tax-enrolments.enabled" -> "true"
      )
    ).build()
  }

  lazy val mcc: MessagesControllerComponents = app.injector.instanceOf[MessagesControllerComponents]

  val reactiveMongoApi = new DefaultReactiveMongoApi(
    parsedUri = MongoConnection.parseURI(mongoUri).success.value,
    dbName = databaseName,
    strictMode = false,
    configuration = configuration,
    new DefaultApplicationLifecycle
  )

  implicit val defaultPatience: PatienceConfig = PatienceConfig(timeout = 5.seconds, interval = 100.millis)

  val fakeAuthorisedAction = new FakeAuthorisedAction(
    mcc,
    layoutInstance,
    authConnector
  )(
    appConfig,
    implicitly,
    messagesApi)

  implicit val lang = Lang("en")
}