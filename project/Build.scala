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
      libraryDependencies ++= Seq(
        "org.clapper" %% "grizzled-slf4j" % "1.0.2",
        "ch.qos.logback" % "logback-core" % "1.1.3",
        "ch.qos.logback" % "logback-classic" % "1.1.3",
        "com.typesafe" % "config" % "1.3.0" withSources(),
        "com.typesafe.akka" %% "akka-actor" % "2.3.9" withSources(),
        "com.rabbitmq" % "amqp-client" % "3.5.3" withSources(),
        "org.scalactic" %% "scalactic" % "2.2.4" withSources()
      ),
      libraryDependencies ++= Seq(
        "org.specs2" %% "specs2-core" % "3.6.2" % "test, it" withSources(),
        "org.specs2" %% "specs2-mock" % "3.6.2" % "test, it" withSources(),
        "org.specs2" %% "specs2-matcher-extra" % "3.6.2" % "test, it" withSources()))

  def existsLocallyAndNotOnJenkins(filePath: String) = {
    new java.io.File(filePath).exists && !new java.io.File(filePath + "/nextBuildNumber").exists()
  }

  def checkFileExistsInADirectoryBelow(filePath: String, times: Int = 0): String = {
    if (times > 3 || existsLocallyAndNotOnJenkins(filePath)) filePath
    else checkFileExistsInADirectoryBelow("../" + filePath, times + 1)
  }

  val rtpiolibPath = checkFileExistsInADirectoryBelow("../rtp-io-lib")

  lazy val root = if (existsLocallyAndNotOnJenkins(rtpiolibPath)) {
    println("=====================")
    println("Build Locally rabbit ")
    println("=====================")

    val actualRtpIoPath = "../rtp-io-lib" // NEEDS TO BE THIS PATH
    val rtpiolib = ProjectRef(file(actualRtpIoPath), "rtp-io-lib")
    rabbit.dependsOn(rtpiolib)
  } else {
    println("=====================")
    println("Build Jenkins rabbit ")
    println("=====================")

    rabbit.settings(
      libraryDependencies ++= Seq(
        "uk.gov.homeoffice" %% "rtp-io-lib" % "1.0-SNAPSHOT" withSources()
      )
    )
  }
}