package uk.gov.hmrc.digitalservicestax.util

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import uk.gov.hmrc.play.bootstrap.auth.DefaultAuthConnector

import scala.concurrent.ExecutionContext

trait WiremockServer extends FakeApplicationSetup with BeforeAndAfterEach with BeforeAndAfterAll with ScalaFutures {
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
