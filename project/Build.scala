import sbt.Keys._
import sbt._

object Build extends Build {
  val moduleName = "rtp-rabbit-lib"

  lazy val root = Project(id = moduleName, base = file("."))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      name := moduleName,
      organization := "uk.gov.homeoffice",
      version := "1.0-SNAPSHOT",
      scalaVersion := "2.11.6",
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
      libraryDependencies ++= Seq(
        "com.typesafe" % "config" % "1.2.1" withSources(),
        "com.typesafe.akka" %% "akka-actor" % "2.3.9" withSources(),
        "com.rabbitmq" % "amqp-client" % "3.5.0" withSources(),
        "org.scalactic" %% "scalactic" % "2.2.4" withSources(),
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" withSources()),
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2-core" % "3.6" % "test, it" withSources(),
        "org.specs2" %% "specs2-mock" % "3.6" % "test, it" withSources(),
        "org.specs2" %% "specs2-matcher-extra" % "3.6" % "test, it" withSources()))
}