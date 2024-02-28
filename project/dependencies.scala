import sbt._

object V {
  val cats    = "2.10.0"
  val ce      = "3.5.2"
  val circe   = "0.14.6"
  val config  = "1.4.3"
  val http4s  = "0.23.23"
  val l4c     = "2.6.0"
  val logback = "1.4.11"
  val slf4j   = "2.0.9"
  val questdb = "7.3.10"
}

object VT {
  val munit = "1.0.0-M10"
  val muce  = "2.0.0-M4"
}

object D {
  val conf = Seq(
    "com.typesafe" % "config"       % "1.4.3",
    "io.circe"    %% "circe-config" % "0.10.1"
  )

  val cats = Seq(
    "org.typelevel" %% "cats-core"   % V.cats,
    "org.typelevel" %% "cats-effect" % V.ce
  )

  val http4s = Seq(
    "org.http4s" %% "http4s-ember-client",
    "org.http4s" %% "http4s-ember-server",
    "org.http4s" %% "http4s-dsl"
  ).map(_ % V.http4s)

  val circe = Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % V.circe)

  val logging = Seq(
    "org.typelevel" %% "log4cats-slf4j"  % V.l4c,
    "org.slf4j"      % "slf4j-api"       % V.slf4j,
    "ch.qos.logback" % "logback-classic" % V.logback
  )

  val persistence = Seq(
    "org.questdb" % "questdb" % V.questdb
  )

  val munit = Seq(
    "org.scalameta" %% "munit"             % VT.munit,
    "org.typelevel" %% "munit-cats-effect" % VT.muce
  ).map(_ % Test)

  val tests = munit
}
