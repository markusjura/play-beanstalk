import NativePackagerKeys._
import com.typesafe.sbt.SbtNativePackager._

name := "play-beanstalk"

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

// Uses Ubuntu with the OpenJDK 7 docker image as a base image: https://github.com/dockerfile/java
dockerBaseImage in Docker := "dockerfile/java"