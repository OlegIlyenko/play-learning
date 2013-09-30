name := "play-book"

version := "1.0-SNAPSHOT"

//scalaVersion := "2.10.2"

libraryDependencies ++= Seq(
  cache,
  "com.github.scaldi" %% "scaldi-play" % "0.2.2",
  "org.neo4j" % "neo4j" % "1.9.2"
)     

play.Project.playScalaSettings