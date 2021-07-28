name := "akka-essentials"

version := "0.1"

scalaVersion := "2.13.6"

val akkaVersion = "2.6.15"
val scalaTestVersion = "3.2.9"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "org.scalatest" %% "scalatest" % scalaTestVersion,
)