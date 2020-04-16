import sbt._

object Dependencies {
  object Versions {
    val pureConfig = "0.12.3"
    val circe      = "0.12.3"
    val http4s     = "0.21.3"
    val mockito    = "1.10.3"
  }

  object Libraries {
    def circe(artifact: String): ModuleID      = "io.circe"              %% artifact % Versions.circe
    def pureConfig(artifact: String): ModuleID = "com.github.pureconfig" %% artifact % Versions.pureConfig
    def http4s(artifact: String): ModuleID     = "org.http4s"            %% artifact % Versions.http4s
    def mockito(artifact: String): ModuleID    = "org.mockito"           %% artifact % Versions.mockito

    val cats       = "org.typelevel"    %% "cats-core"   % "2.1.0"
    val catsEffect = "org.typelevel"    %% "cats-effect" % "2.1.2"
    val catsRetry  = "com.github.cb372" %% "cats-retry"  % "1.1.0"
    val fs2        = "co.fs2"           %% "fs2-core"    % "2.3.0"

    val pureConfigCore = pureConfig("pureconfig")
    val pureConfigCats = pureConfig("pureconfig-cats-effect")

    val circeCore    = circe("circe-core")
    val circeGeneric = circe("circe-generic")
    val circeParser  = circe("circe-parser")
    val circeRefined = circe("circe-refined")

    val http4sCore    = http4s("http4s-core")
    val http4sDsl     = http4s("http4s-dsl")
    val http4sServer  = http4s("http4s-blaze-server")
    val http4sClient  = http4s("http4s-blaze-client")
    val http4sCirce   = http4s("http4s-circe")
    val http4sJwtAuth = "dev.profunktor" %% "http4s-jwt-auth" % "0.0.4"

    val logback  = "ch.qos.logback"    % "logback-classic" % "1.2.3"
    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1"
    val squants  = "org.typelevel"     %% "squants"        % "1.6.0"

    val scalaTest        = "org.scalatest" %% "scalatest" % "3.1.1"
    val catsEffectTest   = "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.0"
    val mockitoCore      = mockito("mockito-scala")
    val mockitoScalatest = mockito("mockito-scala-scalatest")
  }
}
