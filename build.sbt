name := """boldradius-bundle-pricing"""

version := "0.0.0"

scalaVersion := "2.11.7"

scalacOptions in ThisBuild ++=
  "-target:jvm-1.8" ::
    "-feature" ::
    Nil

scalacOptions in Test ++=
  Nil

/** Compile and runtime dependencies. */
libraryDependencies ++=
  "ch.qos.logback" % "logback-classic" % "1.1.3" ::
    "com.squants" %% "squants" % "0.5.3" ::
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0" ::
    Nil

/** Test dependencies. */
libraryDependencies ++=
  Nil
