import play.sbt.PlayImport.ws
import sbt.Defaults.testTasks
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.ForkedJvmPerTestSettings.oneForkedJvmPerTest
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "digital-services-tax-frontend"

PlayKeys.playDefaultPort := 8740

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
scalaVersion := "2.12.11"
ThisBuild / scalacOptions += "-Ypartial-unification"
routesImport += "uk.gov.hmrc.digitalservicestax.data._"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test ++ Seq(ws),
    scalacOptions += "-Ypartial-unification"
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(itSettings)
  .settings(unitTestSettings)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.testSettings): _*)
  .settings(
    resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo),
    scalacOptions -= "-Xfatal-warnings", // Fail the compilation if there are any warnings.
    TwirlKeys.templateImports ++= Seq(
      "ltbs.uniform.{Input => UfInput, _}",
      "ltbs.uniform.common.web.{Breadcrumbs => UfBreadcrumbs, _}",
      "ltbs.uniform.interpreters.playframework._",
      "uk.gov.hmrc.digitalservicestax.views.html.helpers._",
      "uk.gov.hmrc.digitalservicestax.views.html.Layout",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.digitalservicestax.views.AdaptMessages.ufMessagesToPlayMessages",
      "uk.gov.hmrc.digitalservicestax.data._"
    )
  )
  .settings(scoverageSettings)

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*views.*;.*prod.*;.*test.*;app.routes;testOnlyDoNotUseInAppConf;prod.*;controllers.javascript;com.kenshoo.play.metrics.*;",
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*BuildInfo.*;.*Routes.*;testOnlyDoNotUseInAppConf;",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val unitTestSettings = inConfig(Test)(testTasks) ++ Seq(
  Test / testOptions := Seq(Tests.Filter(name => name startsWith "unit")),
  Test / unmanagedSourceDirectories := Seq((baseDirectory in Test).value / "test"),
  Test / parallelExecution := true,
  Test / fork := true,
  addTestReportOption(Test, "unit-test-reports")
)

lazy val itSettings = inConfig(IntegrationTest)(testTasks) ++ Seq(
  IntegrationTest / testOptions := Seq(Tests.Filter(name => name startsWith "it")),
  IntegrationTest / parallelExecution := false,
  IntegrationTest / fork := false,
  addTestReportOption(IntegrationTest, "integration-test-reports")
)