import play.core.PlayVersion.current
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val uniformVersion = "5.0.0-RC2"
  val hmrcMongoVersion = "0.71.0"
  val play = "28"

  val compile = Seq(
    "uk.gov.hmrc"             %% "play-frontend-hmrc"             % s"3.24.0-play-$play",
    "com.chuusai"             %% "shapeless"                      % "2.3.9",
    "com.beachape"            %% "enumeratum-play-json"           % "1.7.0",
    "com.luketebbs.uniform"   %% s"interpreter-play$play"         % uniformVersion,
    "uk.gov.hmrc.mongo"       %% s"hmrc-mongo-play-$play"         % hmrcMongoVersion,
    "commons-validator"       %  "commons-validator"              % "1.7",
    "uk.gov.hmrc"             %% s"bootstrap-frontend-play-$play" % "7.3.0",
    "fr.marcwrobel"           %  "jbanking"                       % "3.4.0",
    "org.jsoup"               %  "jsoup"                          % "1.15.3",
    "joda-time"               %  "joda-time"                      % "2.11.1" // needed for hmrc-mongo
  )

  val test = Seq(
    "org.scalatest"                 %% "scalatest"                    % "3.2.13",
    "org.jsoup"                     %  "jsoup"                        % "1.15.3",
    "com.typesafe.play"             %% "play-test"                    % current,
    "org.scalacheck"                %% "scalacheck"                   % "1.16.0",
    "uk.gov.hmrc"                   %% "stub-data-generator"          % "0.5.3",
    "io.chrisdavenport"             %% "cats-scalacheck"              % "0.3.1",
    "com.beachape"                  %% "enumeratum-scalacheck"        % "1.7.0",
    "wolfendale"                    %% "scalacheck-gen-regexp"        % "0.1.2",
    "com.outworkers"                %% "util-samplers"                % "0.57.0",
    "com.github.tomakehurst"        %  "wiremock-jre8"                % "2.33.2",
    "com.fasterxml.jackson.module"  %% "jackson-module-scala"         % "2.13.4",
    "uk.gov.hmrc.mongo"             %% s"hmrc-mongo-test-play-$play"  % hmrcMongoVersion,
    "org.pegdown"                   %  "pegdown"                      % "1.6.0",
    "com.vladsch.flexmark"          %  "flexmark-all"                 % "0.62.2",
    "org.scalatestplus.play"        %% "scalatestplus-play"           % "5.1.0",
    "org.scalatestplus"             %% "scalacheck-1-15"              % "3.2.11.0",
    "org.scalatestplus"             %% "mockito-3-12"                 % "3.2.10.0",
    "org.mockito"                   %  "mockito-core"                 % "4.8.0",
    "com.luketebbs.uniform"         %% "interpreter-logictable"       % uniformVersion
  ).map (_ % Test)

}
