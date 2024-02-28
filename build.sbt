import D._
import sbt.Keys._

ThisBuild / scalaVersion := "3.3.3"
ThisBuild / organization := "net.gemelen.dev.narrativeio"

ThisBuild / semanticdbEnabled := true
ThisBuild / scalacOptions ++=
  Seq(
    "-encoding",
    "utf8",
    "-deprecation",
    "-no-indent",
    "-Werror",
    "-Wunused:all",
    "-Wvalue-discard",
    "-source",
    "3.3-migration",
    "-rewrite"
  )

ThisBuild / Compile / run / fork := true
ThisBuild / assemblyMergeStrategy := {
  case "module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (ThisBuild / assemblyMergeStrategy).value
    oldStrategy(x)
}
lazy val challenge = project
  .in(file("."))
  .settings(
    name                       := "challenge",
    assembly / mainClass       := Some("net.gemelen.dev.narrativeio.EntryPoint"),
    assembly / assemblyJarName := "narrativeio-challenge.jar",
    libraryDependencies :=
      conf ++ cats ++ circe ++ http4s ++ logging ++ persistence
  )
