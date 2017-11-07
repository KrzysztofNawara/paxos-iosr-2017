
val akkaVersion = "2.5.6"
val logbackVersion = "1.2.3"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.3",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "Hello",
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion,

    libraryDependencies += "com.typesafe.akka" %% "akka-testkit"  % akkaVersion % Test,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.3" % Test,
  )
