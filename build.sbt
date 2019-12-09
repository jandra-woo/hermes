ThisBuild / organization := "com.jandra"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "2.12.8"

lazy val akkaVersion = "2.6.0"
lazy val akkaHttpVersion = "10.1.10"
lazy val circeVersion = "0.10.0"
lazy val aecorVersion = "0.18.0"

scalacOptions ++= Seq(
    "-Ypartial-unification"
  )

addCompilerPlugin("org.scalamacros" % "paradise_2.12.8" % "2.1.1")

lazy val hermes =
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

    //akka-cluster-typed
    "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

    //akka-persistence
    "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    "com.typesafe.akka" %% "akka-serialization-jackson" % akkaVersion,

    //akka-stream
    "com.typesafe.akka" %% "akka-stream" % akkaVersion,

    //akka-http
    "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
    "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
    
    //akka-http-json4s
    "de.heikoseeberger" %% "akka-http-json4s" % "1.29.1", 
    
    //json4s
    "org.json4s" %% "json4s-jackson" % "3.7.0-M1",
    "org.json4s" %% "json4s-ext" % "3.7.0-M1",

    //akka-management
//    "com.lightbend.akka.management" %% "akka-management" % "1.0.1",

    //akka-testkit
//    "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,

    //akka-testkit-typed
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
    "org.scalatest" %% "scalatest" % "3.0.8" % "test",

    //alpakka-slick
    "com.lightbend.akka" %% "akka-stream-alpakka-slick" % "1.0-RC1",

    //slick
    "com.typesafe.slick" %% "slick" % "3.3.1",
    "com.typesafe.slick" %% "slick-hikaricp" % "3.3.1",
    "org.postgresql" % "postgresql" % "42.2.1",

    //logback
    "ch.qos.logback" % "logback-classic" % "1.2.3",

    //spray-json
    "io.spray" %% "spray-json" % "1.3.4",

    //level-db
    "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8",

    //Aecor
//    "io.aecor" %% "core" % aecorVersion,
//    "io.aecor" %% "schedule" % aecorVersion,
//    "io.aecor" %% "akka-cluster-runtime" % aecorVersion,
//    "io.aecor" %% "akka-persistence-runtime" % aecorVersion,
//    "io.aecor" %% "distributed-processing" % aecorVersion,
//    "io.aecor" %% "boopickle-wire-protocol" % aecorVersion,
//    "io.aecor" %% "test-kit" % aecorVersion % Test
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
    mainClass in(Compile, run) := Some("com.jandra.hermes.order.OrderMain"),
    test in assembly := {},
    mainClass in assembly := Some("com.jandra.hermes.order.OrderMain")
  )

lazy val facility = project
  .in(file("facility"))
  .dependsOn(order)
  .settings(
    baseSettings,
    mainClass in(Compile, run) := Some("com.jandra.hermes.FacilityMain"),
    test in assembly := {},
    mainClass in assembly := Some("com.jandra.hermes.FacilityMain")
  )
