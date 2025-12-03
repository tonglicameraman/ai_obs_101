enablePlugins(GatlingPlugin)

scalaVersion := "2.13.16"

scalacOptions := Seq(
  "-encoding",
  "UTF-8",
  "-release:8",
  "-deprecation",
  "-feature",
  "-unchecked",
  "-language:implicitConversions",
  "-language:postfixOps"
)

val gatlingVersion = "3.13.5"
val circeVersion = "0.14.12"

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion % "test,it",
  "io.gatling" % "gatling-test-framework" % gatlingVersion % "test,it",
  "com.thedeanda" % "lorem" % "2.2"
) ++ Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
