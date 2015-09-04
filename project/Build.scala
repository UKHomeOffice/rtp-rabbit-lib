import sbt.Keys._
import sbt._

object Build extends Build {
  val moduleName = "rtp-rabbit-lib"

  lazy val rabbit = Project(id = moduleName, base = file("."))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
      version := "1.0-SNAPSHOT",
      scalaVersion := "2.11.7",
      scalacOptions ++= Seq(
        "-feature",
        "-language:implicitConversions",
        "-language:higherKinds",
        "-language:existentials",
        "-language:reflectiveCalls",
        "-language:postfixOps",
        "-Yrangepos",
        "-Yrepl-sync"),
      resolvers ++= Seq(
        "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/",
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "scalaz-bintray" at "https://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io"),
      mappings in (Test, packageBin) ~= { _.filter(!_._1.getName.endsWith("logback.xml")) },
      libraryDependencies ++= Seq(
        "org.clapper" %% "grizzled-slf4j" % "1.0.2",
        "ch.qos.logback" % "logback-core" % "1.1.3",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.typesafe" % "config" % "1.3.0" withSources(),
        "com.typesafe.akka" %% "akka-actor" % "2.3.12" withSources(),
        "com.rabbitmq" % "amqp-client" % "3.5.4" withSources()
      ),
      libraryDependencies ++= Seq(
        "com.typesafe.akka" %% "akka-testkit" % "2.3.12" % Test withSources()))

  val testPath = "../rtp-test-lib"
  val ioPath = "../rtp-io-lib"
  val akkaPath = "../rtp-akka-lib"

  val root = if (file(testPath).exists && sys.props.get("jenkins").isEmpty) {
    println("=====================")
    println("Build Locally domain ")
    println("=====================")

    val testLib = ProjectRef(file(testPath), "rtp-test-lib")
    val ioLib = ProjectRef(file(ioPath), "rtp-io-lib")
    val akkaLib = ProjectRef(file(akkaPath), "rtp-akka-lib")

    rabbit.dependsOn(testLib % "test->test;compile->compile")
          .dependsOn(ioLib % "test->test;compile->compile")
          .dependsOn(akkaLib % "test->test;compile->compile")

  } else {
    println("========================")
    println("Build on Jenkins domain ")
    println("========================")

    rabbit.settings(
      libraryDependencies ++= Seq(
        "uk.gov.homeoffice" %% "rtp-test-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-test-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % "1.0-SNAPSHOT" withSources(),
        "uk.gov.homeoffice" %% "rtp-akka-lib" % "1.0-SNAPSHOT" % Test classifier "tests" withSources() excludeAll ExclusionRule(organization = "org.specs2")
      ))
  }
}