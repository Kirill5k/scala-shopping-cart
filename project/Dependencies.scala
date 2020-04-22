import sbt._

object Dependencies {
  object Versions {
    val cats       = "2.1.1"
    val pureConfig = "0.12.3"
    val circe      = "0.12.3"
    val http4s     = "0.21.3"
    val mockito    = "1.10.3"
    val refined    = "0.9.13"
    val redis4cats = "0.9.6"

    val scalaCheck    = "1.14.3"
    val scalaTest     = "3.1.1"
    val scalaTestPlus = "3.1.1.1"
  }

  object Libraries {
    def circe(artifact: String): ModuleID      = "io.circe"              %% artifact % Versions.circe
    def pureConfig(artifact: String): ModuleID = "com.github.pureconfig" %% artifact % Versions.pureConfig
    def http4s(artifact: String): ModuleID     = "org.http4s"            %% artifact % Versions.http4s
    def mockito(artifact: String): ModuleID    = "org.mockito"           %% artifact % Versions.mockito
    def refined(artifact: String): ModuleID    = "eu.timepit"            %% artifact % Versions.refined
    def redis(artifact: String): ModuleID      = "dev.profunktor"        %% artifact % Versions.redis4cats

    val catsCore    = "org.typelevel"    %% "cats-core"              % "2.1.1"
    val catsTestkit = "org.typelevel"    %% "cats-testkit-scalatest" % "1.0.1"
    val catsEffect  = "org.typelevel"    %% "cats-effect"            % "2.1.2"
    val catsRetry   = "com.github.cb372" %% "cats-retry"             % "1.1.0"
    val fs2         = "co.fs2"           %% "fs2-core"               % "2.3.0"

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

    val refinedCore = refined("refined")
    val refinedCats = refined("refined-cats")

    val redis4catsCore   = redis("redis4cats-effects")
    val redis4catsStream = redis("redis4cats-streams")
    val redis4catsLogs   = redis("redis4cats-log4cats")

    val logback  = "ch.qos.logback"    % "logback-classic" % "1.2.3"
    val log4cats = "io.chrisdavenport" %% "log4cats-slf4j" % "1.0.1"
    val squants  = "org.typelevel"     %% "squants"        % "1.6.0"

    val scalaCheck    = "org.scalacheck"    %% "scalacheck"      % Versions.scalaCheck
    val scalaTest     = "org.scalatest"     %% "scalatest"       % Versions.scalaTest
    val scalaTestPlus = "org.scalatestplus" %% "scalacheck-1-14" % Versions.scalaTestPlus

    val catsEffectTest   = "com.codecommit" %% "cats-effect-testing-scalatest" % "0.4.0"
    val mockitoCore      = mockito("mockito-scala")
    val mockitoScalatest = mockito("mockito-scala-scalatest")
    val redisEmbedded    = "com.github.sebruck" %% "scalatest-embedded-redis" % "0.4.0"
  }
}
