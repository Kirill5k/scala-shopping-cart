import Dependencies.Libraries._

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "io.github.kirill5k"
ThisBuild / organizationName := "kirill5k"

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publish / skip := true
)

lazy val dockerSettings = Seq(
  Docker / packageName := "shopping-cart",
  Docker / version := sys.env.getOrElse("APP_VERSION", version.value),
  dockerBaseImage := "openjdk:11.0.4-jre-slim",
  dockerExposedPorts ++= Seq(8080),
  dockerUpdateLatest := true,
  makeBatScripts := Seq()
)

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name := "shopping-cart"
  )
  .aggregate(core)

lazy val core = (project in file("modules/core"))
  .enablePlugins(DockerPlugin, AshScriptPlugin)
  .settings(dockerSettings)
  .settings(
    name := "shopping-cart-core",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    libraryDependencies ++= Seq(
      pureConfigCore,
      pureConfigCats,
      logback % Runtime,
      log4cats,
      circeCore,
      circeGeneric,
      circeGenericExtras,
      circeParser,
      circeLiteral,
      circeRefined,
      http4sCore,
      http4sDsl,
      http4sServer,
      http4sClient,
      http4sCirce,
      http4sJwtAuth,
      bcrypt,
      squants,
      redis4catsCore,
      redis4catsStream,
      redis4catsLogs,
      refinedCore,
      refinedCats,
      skunkCore,
      skunkCirce,
      scalaTest        % Test,
      scalaTestPlus    % Test,
      scalaCheck       % Test,
      catsTestkit      % Test,
      catsEffectTest   % Test,
      mockitoCore      % Test,
      mockitoScalatest % Test,
      redisEmbedded    % Test,
      testContainersScalatest % Test,
      testContainersPostgres % Test,
      postgresDriver % Test
    )
  )
