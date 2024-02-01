import play.core.PlayVersion.current
import sbt._

object AppDependencies {

  val uniformVersion           = "5.0.0-RC2"
  val hmrcMongoVersion         = "1.7.0"
  val play                     = "30"
  private val bootstrapVersion = "8.4.0"

  val compile = Seq(
    "uk.gov.hmrc"           %% s"play-frontend-hmrc-play-$play" % "8.4.0",
    "com.chuusai"           %% "shapeless"                      % "2.4.0-M1",
    "com.beachape"          %% "enumeratum-play-json"           % "1.7.0",
    "com.luketebbs.uniform" %% s"interpreter-play28"            % uniformVersion,
    "uk.gov.hmrc.mongo"     %% s"hmrc-mongo-play-$play"         % hmrcMongoVersion,
    "commons-validator"      % "commons-validator"              % "1.7",
    "uk.gov.hmrc"           %% s"bootstrap-frontend-play-$play" % bootstrapVersion,
    "fr.marcwrobel"          % "jbanking"                       % "4.0.0",
    "org.jsoup"              % "jsoup"                          % "1.15.3",
    "joda-time"              % "joda-time"                      % "2.12.1" // needed for hmrc-mongo
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"                   % "3.2.17",
    "org.jsoup"               % "jsoup"                       % "1.15.4",
    "org.scalacheck"         %% "scalacheck"                  % "1.17.0",
    "io.chrisdavenport"      %% "cats-scalacheck"             % "0.3.2",
    "com.beachape"           %% "enumeratum-scalacheck"       % "1.7.2",
    "wolfendale"             %% "scalacheck-gen-regexp"       % "0.1.2",
    "com.outworkers"         %% "util-samplers"               % "0.57.0",
    "uk.gov.hmrc.mongo"      %% s"hmrc-mongo-test-play-$play" % hmrcMongoVersion,
    "uk.gov.hmrc"            %% s"bootstrap-test-play-$play"  % bootstrapVersion,
    "com.vladsch.flexmark"    % "flexmark-all"                % "0.64.6",
    "org.scalatestplus.play" %% "scalatestplus-play"          % "7.0.0",
    "org.scalatestplus"      %% "scalacheck-1-17"             % "3.2.17.0",
    "org.scalatestplus"      %% "mockito-3-12"                % "3.2.10.0",
    "org.mockito"             % "mockito-core"                % "5.2.0",
    "com.luketebbs.uniform"  %% "interpreter-logictable"      % uniformVersion
  ).map(_ % Test)

}
