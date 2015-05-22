import sbt.Keys._
import sbt._

object Build extends Build {
  lazy val root = Project(id = "rabb-it", base = file("."))
    .configs(IntegrationTest)
    .settings(Defaults.itSettings: _*)
    .settings(
      name := "rabb-it",
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
        "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
        "Sonatype Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots/",
        "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases",
        "Kamon Repository" at "http://repo.kamon.io",
        "Artifactory Snapshot Realm" at "http://artifactory.registered-traveller.homeoffice.gov.uk/artifactory/libs-snapshot-local/"),
      libraryDependencies ++= Seq(
        "com.typesafe" % "config" % "1.2.1" withSources(),
        "com.typesafe.akka" %% "akka-actor" % "2.3.9" withSources(),
        "com.rabbitmq" % "amqp-client" % "3.5.0" withSources(),
        "uk.gov.homeoffice" %% "io-it" % "1.0-SNAPSHOT" withSources()),
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2-core" % "3.6" % "test, it" withSources(),
        "org.specs2" %% "specs2-mock" % "3.6" % "test, it" withSources(),
        "org.specs2" %% "specs2-matcher-extra" % "3.6" % "test, it" withSources()))
}