import sbt._

object Dependencies {
  lazy val pureConfigVersion = "0.12.3"
  lazy val circeVersion      = "0.12.3"
  lazy val http4sVersion     = "0.21.3"

  lazy val pureConfig     = "com.github.pureconfig" %% "pureconfig"             % pureConfigVersion
  lazy val pureConfigCats = "com.github.pureconfig" %% "pureconfig-cats-effect" % pureConfigVersion

  lazy val cats       = "org.typelevel" %% "cats-core"   % "2.1.0"
  lazy val catsEffect = "org.typelevel" %% "cats-effect" % "2.1.2"
  lazy val fs2        = "co.fs2"        %% "fs2-core"    % "2.3.0"

  lazy val logback  = "ch.qos.logback"    % "logback-classic" % "1.2.3"
  lazy val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1"

  lazy val circe        = "io.circe" %% "circe-core"    % circeVersion
  lazy val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  lazy val circeParser  = "io.circe" %% "circe-parser"  % circeVersion

  lazy val http4s       = "org.http4s" %% "http4s-core"         % http4sVersion
  lazy val http4sDsl    = "org.http4s" %% "http4s-dsl"          % http4sVersion
  lazy val http4sServer = "org.http4s" %% "http4s-server"       % http4sVersion
  lazy val http4sBlaze  = "org.http4s" %% "http4s-blaze-server" % http4sVersion
  lazy val http4sCirce  = "org.http4s" %% "http4s-circe"        % http4sVersion

  lazy val scalaTest      = "org.scalatest"  %% "scalatest"                     % "3.1.1"
  lazy val catsEffectTest = "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.0"
}
