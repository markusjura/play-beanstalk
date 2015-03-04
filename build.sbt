import NativePackagerKeys._
import com.typesafe.sbt.SbtNativePackager._

name := "play-docker-beanstalk"

version := "1.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  ws
)

// Docker settings
maintainer in Docker := "Your Name <your@email.com>"

// Port which will be accessible outside of the docker image
dockerExposedPorts in Docker := Seq(9000)

// Uses the standard java 8 oracle docker images as a base image
dockerBaseImage in Docker := "dockerfile/oracle-java8"