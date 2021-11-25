package uk.gov.hmrc.digitalservicestax.util

import org.scalatestplus.mockito.MockitoSugar
import play.api.{Configuration, Environment}

import java.io.File
import java.time.Clock

trait TestWiring extends MockitoSugar {
  val appName: String = configuration.get[String]("appName")

  lazy val configuration: Configuration = Configuration.load(environment, Map(
    "auditing.enabled" -> "false",
    "services.auth.port" -> "11111"
  ))
  lazy val environment: Environment = Environment.simple(new File("."))
  lazy val mode = environment.mode

  implicit def clock: Clock = Clock.systemDefaultZone()
}
