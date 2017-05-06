name := "tsefb"

version := "1.0"

scalaVersion := "2.12.1"

val AkkaVersion = "2.5.1"
val AkkaHttpVersion = "10.0.5"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  "com.github.scribejava" % "scribejava-apis" % "4.1.0",

  // Provides parsing of a stream of Akka ByteStrings to JSON.
  "de.knutwalker" %% "akka-stream-json" % "3.3.0",
  "de.knutwalker" %% "akka-http-circe" % "3.3.0"
)

scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
)

// wartremoverWarnings ++= Warts.allBut(Wart.NonUnitStatements, Wart.Overloading)
