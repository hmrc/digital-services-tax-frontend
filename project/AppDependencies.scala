import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val uniformVersion           = "5.0.0-RC2"
  val hmrcMongoVersion         = "2.7.0"
  val play                     = "play-30"
  private val bootstrapVersion = "10.1.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-$play" % "12.8.0",
    "com.chuusai"           %% "shapeless"                 % "2.4.0-M1",
    "com.beachape"          %% "enumeratum-play-json"      % "1.8.0",
    "com.luketebbs.uniform" %% s"interpreter-play28"       % uniformVersion, // The most recent edition of the uniform is play 28, and the play 30 version has not been released yet.
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-$play"         % hmrcMongoVersion,
    "commons-validator"      % "commons-validator"         % "1.8.0",
    "uk.gov.hmrc"           %% s"bootstrap-frontend-$play" % bootstrapVersion,
    "fr.marcwrobel"          % "jbanking"                  % "4.2.0",
    "org.jsoup"              % "jsoup"                     % "1.17.2"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"              % "3.2.18",
    "org.scalacheck"         %% "scalacheck"             % "1.18.0",
    "io.chrisdavenport"      %% "cats-scalacheck"        % "0.3.2",
    "com.beachape"           %% "enumeratum-scalacheck"  % "1.7.3",
    "wolfendale"             %% "scalacheck-gen-regexp"  % "0.1.2",
    "com.outworkers"         %% "util-samplers"          % "0.57.0",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-$play" % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"bootstrap-test-$play"  % bootstrapVersion,
    "com.vladsch.flexmark"    % "flexmark-all"           % "0.64.8",
    "org.scalatestplus.play" %% "scalatestplus-play"     % "7.0.1",
    "org.scalatestplus"      %% "scalacheck-1-17"        % "3.2.18.0",
    "org.scalatestplus"      %% "mockito-3-12"           % "3.2.10.0",
    "org.mockito"             % "mockito-core"           % "5.11.0",
    "com.luketebbs.uniform"  %% "interpreter-logictable" % uniformVersion
  ).map(_ % Test)

}
