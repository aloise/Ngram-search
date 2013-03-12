import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "abc-ngram-search"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
    jdbc,
    anorm,
    "mysql" % "mysql-connector-java" % "5.1.21",
    "org.squeryl" %% "squeryl" % "0.9.+"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    // Add your own project settings here      
  )

}
