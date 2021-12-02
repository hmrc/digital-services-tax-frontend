import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val uniformVersion = "5.0.0-RC2"
  val bootstrapVersion = "5.14.0"
  val hmrcMongoVersion = "0.55.0"
  val play = "28"

  val compile = Seq(
    "uk.gov.hmrc"             %% "play-frontend-hmrc"             % s"1.19.0-play-$play",
    "com.chuusai"             %% "shapeless"                      % "2.3.3",
    "com.beachape"            %% "enumeratum-play-json"           % "1.7.0",
    "com.luketebbs.uniform"   %% s"interpreter-play$play"         % uniformVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-play-$play"         % hmrcMongoVersion,
    "commons-validator"       %  "commons-validator"              % "1.6",
    "uk.gov.hmrc"             %% s"bootstrap-frontend-play-$play" % bootstrapVersion,
    "fr.marcwrobel"           %  "jbanking"                       % "3.1.1",
    "org.jsoup"               %  "jsoup"                          % "1.14.2",
    "joda-time"               %  "joda-time"                      % "2.10.13" // needed for hmrc-mongo
  )

  val test = Seq(
    "org.scalatest"             %% "scalatest"                   % "3.0.8",
    "org.jsoup"                 %  "jsoup"                       % "1.13.1",
    "com.typesafe.play"         %% "play-test"                   % current,
    "org.scalacheck"            %% "scalacheck"                  % "1.15.4",
    "uk.gov.hmrc"               %% "stub-data-generator"         % "0.5.3",
    "io.chrisdavenport"         %% "cats-scalacheck"             % "0.3.1",
    "com.beachape"              %% "enumeratum-scalacheck"       % "1.7.0",
    "wolfendale"                %% "scalacheck-gen-regexp"       % "0.1.2",
    "com.outworkers"            %% "util-samplers"               % "0.57.0",
    "com.github.tomakehurst"    %  "wiremock-jre8"               % "2.27.2", // upgrade blocked by play databind dependency
    "uk.gov.hmrc.mongo"         %% s"hmrc-mongo-test-play-$play" % hmrcMongoVersion,
    "org.pegdown"               %  "pegdown"                     % "1.6.0",
    "org.scalatestplus.play"    %% "scalatestplus-play"          % "5.1.0",
    "org.scalatestplus"         %% "scalacheck-1-15"             % "3.2.10.0",
    "org.scalatestplus"         %% "mockito-3-12"                % "3.2.10.0",
    "org.mockito"               %  "mockito-core"                % "3.3.3",
    "com.luketebbs.uniform"     %% "interpreter-logictable"      % uniformVersion
  ).map (_ % Test)

}
