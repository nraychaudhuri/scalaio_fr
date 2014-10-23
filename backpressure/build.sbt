name := """backpressure"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  ws,
  "com.nworks" % "nbbmq_2.11" % "0.1-SNAPSHOT",
  "com.typesafe.akka" %% "akka-stream-experimental" % "0.6"
)
