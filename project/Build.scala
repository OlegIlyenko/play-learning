import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "play-book"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "com.github.scaldi" %% "scaldi-play" % "0.2.1",
    "org.neo4j" % "neo4j" % "1.9.2"
  )

  val main = play.Project(appName, appVersion, appDependencies)
    .settings(
      scalaVersion := "2.10.2"
    )
    .settings(net.virtualvoid.sbt.graph.Plugin.graphSettings: _*)

}
