name := "tse"

version := "1.0"

lazy val root = project in file(".")

scalaVersion := "2.12.1"

val AkkaVersion = "2.5.1"
val AkkaHttpVersion = "10.0.5"

resolvers += "krasserm at bintray" at "http://dl.bintray.com/krasserm/maven"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-stream-testkit" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,

  "org.scalactic" %% "scalactic" % "3.0.1",
  "org.scalatest" %% "scalatest" % "3.0.1" % "test",

  "com.github.scribejava" % "scribejava-apis" % "4.1.0",

  // Provides parsing of a stream of Akka ByteStrings to JSON.
  "de.knutwalker" %% "akka-stream-json" % "3.3.0",
  "de.knutwalker" %% "akka-http-circe" % "3.3.0",

  // For analysis of Json ASTs.
  "io.circe" %% "circe-optics" % "0.8.0",

  "co.fs2" %% "fs2-core" % "0.9.5",
  "co.fs2" %% "fs2-io" % "0.9.5",

  "com.github.krasserm" %% "streamz-converter" % "0.7",

  "org.http4s" %% "http4s-blaze-client" % "0.17.0-M2",
  "org.http4s" %% "http4s-circe" % "0.17.0-M2",
  "io.circe" %% "circe-generic" % "0.8.0"

)

scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
)

wartremoverWarnings ++= Warts.allBut(Wart.Any)
