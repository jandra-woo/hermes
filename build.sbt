ThisBuild / organization := "com.jandra"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"

lazy val akkaVersion = "2.5.23"
lazy val akkaHttpVersion = "10.1.8"
lazy val circeVersion = "0.10.0"
lazy val aecorVersion = "0.18.0"

scalacOptions ++= Seq(
    "-Ypartial-unification"
  )

addCompilerPlugin("org.scalamacros" % "paradise_2.12.8" % "2.1.1")

lazy val root =
  project
    .in(file("."))
    .aggregate(order, facility, common)

lazy val baseSettings = Seq(
  fork in run := true,

  libraryDependencies ++= Seq(
    //avro & avro4s
    "org.apache.avro" % "avro" % "1.9.0",
    "com.sksamuel.avro4s" %% "avro4s-core" % "3.0.0-RC2",

    //circe
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,

    //akka-cluster
    "com.typesafe.akka" %% "akka-cluster" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-tools" % akkaVersion,
    "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding" % akkaVersion,

    //akka-stream
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,

    //akka-http
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

    //akka-management
    "com.lightbend.akka.management" %% "akka-management" % "1.0.1",

    //akka-testkit
    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

    //alpakka-slick
    "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "1.0-RC1",
    "org.scalatest" %% "scalatest" % "3.0.5" % "test",

    //slick
    "com.typesafe.slick" %% "slick" % "3.2.3",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.2.3",
    "org.postgresql" % "postgresql" % "42.2.1",

    //logback
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    //spray-json
    "io.spray" %% "spray-json" % "1.3.4",

    //level-db
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

    //Aecor
    "io.aecor" %% "core" % aecorVersion,
    "io.aecor" %% "schedule" % aecorVersion,
    "io.aecor" %% "akka-cluster-runtime" % aecorVersion,
    "io.aecor" %% "akka-persistence-runtime" % aecorVersion,
    "io.aecor" %% "distributed-processing" % aecorVersion,
    "io.aecor" %% "boopickle-wire-protocol" % aecorVersion,
    "io.aecor" %% "test-kit" % aecorVersion % Test
  )
 
)

lazy val common = project
  .in(file("common"))
  .settings(
    baseSettings
  )

lazy val order = project
  .in(file("order"))
  .dependsOn(common)
  .settings(
    baseSettings,
    mainClass in(Compile, run) := Some("com.jandra.hermes.order.OrderMain")
  )

lazy val facility = project
  .in(file("facility"))
  .settings(
    baseSettings
  )
