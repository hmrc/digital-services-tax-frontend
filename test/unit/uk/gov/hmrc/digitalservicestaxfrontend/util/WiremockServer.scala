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

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.ExecutionContext

trait WiremockServer extends FakeApplicationServer with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
  val port: Int = 11111

  protected[this] val mockServer = new WireMockServer(port)

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  val mockServerUrl = s"http://localhost:$port"

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    mockServer.start()
    WireMock.configureFor("localhost", port)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    WireMock.reset()
  }

  override protected def afterAll(): Unit = {
    super.afterAll()
    mockServer.stop()
  }

  val fakeAuthConnector = new DefaultAuthConnector(httpClient, servicesConfig) {
    override val serviceUrl = mockServerUrl
  }
}
