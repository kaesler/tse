name := "tsefb"

version := "1.0"

scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream" % "2.5.1",
  "com.typesafe.akka" %% "akka-stream-testkit" % "2.5.1",
  "com.typesafe.akka" %% "akka-http" % "10.0.5",
  "org.json4s" %% "json4s-native" % "3.5.0",
  "org.scalactic" %% "scalactic" % "3.0.1",
  "com.github.scribejava" % "scribejava-apis" % "4.1.0"
)

scalacOptions ++= Seq(
    "-feature",
    "-deprecation"
)

// wartremoverWarnings ++= Warts.allBut(Wart.NonUnitStatements, Wart.Overloading)
