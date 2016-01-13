name := """boldradius-bundle-pricing"""

version := "0.0.0"

scalaVersion := "2.11.7"

scalacOptions ++=
  "-feature" ::
    "-target:jvm-1.8" ::
    Nil

scalacOptions in Test ++=
  Nil

/** Compile and runtime dependencies. */
libraryDependencies ++=
  "ch.qos.logback" % "logback-classic" % "1.1.3" ::
    "com.chuusai" %% "shapeless" % "2.2.5" ::
    "com.squants" %% "squants" % "0.5.3" ::
    // "com.typesafe.akka" %% "akka-actor" % "2.4.1" ::
    "com.typesafe.play" %% "play-json" % "2.4.6" ::
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0" ::
    "io.dropwizard.metrics" % "metrics-core" % "3.1.2" ::
    "org.choco-solver" % "choco-solver" % "3.3.3" ::
    // "org.julienrf" %% "play-json-variants" % "2.0" ::
    "org.scalaz" %% "scalaz-core" % "7.2.0" ::
    Nil

/** Test dependencies. */
libraryDependencies ++=
  // "com.typesafe.akka" %% "akka-testkit" % "2.4.1" % "test" ::
    "org.scalatest" %% "scalatest" % "2.2.4" % "test" ::
    Nil

scalacOptions in(Compile, doc) ++=
  "-author" ::
    "-groups" ::
    "-implicits" ::
    Nil

/** See http://scala-sbt.org/0.13/docs/Howto-Scaladoc.html for details. */
autoAPIMappings := true

/** See http://stackoverflow.com/a/20919304/700420 for details. */
apiMappings ++= {
  val cp: Seq[Attributed[File]] = (fullClasspath in Compile).value

  def findManagedDependency(organization: String, name: String): File =
    (for {
      entry <- cp
      module <- entry.get(moduleID.key)
      if module.organization == organization
      if module.name.startsWith(name)
    } yield entry.data).head

  Map(
    findManagedDependency("com.squants", "squants") -> url("http://oss.sonatype.org/service/local/repositories/releases/archive/com/squants/squants_2.11/0.5.3/squants_2.11-0.5.3-javadoc.jar/!"),
    // findManagedDependency("com.typesafe.akka", "akka-actor") -> url("http://doc.akka.io/api/akka/2.4.1/"),
    findManagedDependency("com.typesafe.play", "play-json") -> url("http://playframework.com/documentation/2.4.x/api/scala/"),
    /* FIXME: This doesn't actually work; links get created but they have the wrong path. */
    findManagedDependency("org.choco-solver", "choco-solver") -> url("http://choco-solver.org/apidocs/")
  )
}
