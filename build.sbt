version := "0.1-SNAPSHOT"
organization := "org"
description := "stream-exercise"
scalaVersion := "2.13.12"
isSnapshot := false
scalacOptions := Seq("-unchecked", "-deprecation", "-Xfatal-warnings")
Test / fork := true
Test / parallelExecution := false

val akkaVersion      = "2.6.20"
val fs2Version       = "3.7.0"
val scalaTestVersion = "3.2.17"
val mockitoVersion   = "1.17.27"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream"         % akkaVersion,
  "redis.clients"     %  "jedis"               % "4.3.1",
  "io.spray"          %% "spray-json"          % "1.3.6",
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.scalatest"     %% "scalatest"           % scalaTestVersion % Test,
  "com.dimafeng"      %% "testcontainers-scala-scalatest" % "0.41.5" % Test,
  "com.dimafeng"      %% "testcontainers-scala-redis" % "0.41.5" % Test,
)
