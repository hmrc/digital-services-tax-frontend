import play.sbt.PlayImport.ws
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "digital-services-tax-frontend"

PlayKeys.playDefaultPort := 8740

addCompilerPlugin("org.typelevel" %% "kind-projector" % "0.10.3")
scalaVersion := "2.13.10"
routesImport += "uk.gov.hmrc.digitalservicestax.data._"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    majorVersion := 0,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test ++ Seq(ws)
  )
  .settings(publishingSettings: _*)
  .settings(
    // To resolve a bug with version 2.x.x of the scoverage plugin - https://github.com/sbt/sbt/issues/6997
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always)
  )
  .configs(IntegrationTest)
  .settings(integrationTestSettings ++ unitTestSettings)
  .settings(
    resolvers ++= Seq(Resolver.jcenterRepo),
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
  .settings(
    scalacOptions += "-Wconf:src=routes/.*:s",
    scalacOptions += "-Wconf:cat=unused-imports&src=html/.*:s"
  )

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys
  Seq(
    // Semicolon-separated list of regexs matching classes to exclude
    ScoverageKeys.coverageExcludedPackages := "<empty>;.*views.*;.*prod.*;.*test.*;app.routes;testOnlyDoNotUseInAppConf;prod.*;controllers.javascript;com.kenshoo.play.metrics.*;.*ReturnsRepoMongoImplementation.*;.*DSTConnector.*;.*DSTService.*",
    ScoverageKeys.coverageExcludedFiles := "<empty>;.*BuildInfo.*;.*Routes.*;testOnlyDoNotUseInAppConf;",
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

// If you want integration tests in the test package you need to extend Test config ...
lazy val IntegrationTest = config("it") extend Test

lazy val unitTestSettings = inConfig(Test)(Defaults.testTasks) ++ Seq(
  Test / testOptions := Seq(Tests.Filter(name => name startsWith "unit")),
  Test / fork := true,
  Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test"),
  addTestReportOption(Test, "test-reports")
)

lazy val integrationTestSettings = inConfig(IntegrationTest)(Defaults.testTasks) ++ Seq(
  IntegrationTest / testOptions := Seq(Tests.Filter(name => name startsWith "it")),
  IntegrationTest / fork := false,
  IntegrationTest / parallelExecution := false,
  addTestReportOption(IntegrationTest, "integration-test-reports")
)