import Dependencies.Libraries._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "io.kirill"
ThisBuild / organizationName := "kirill"

lazy val root = (project in file("."))
  .settings(
    name := "scala-shopping-cart",
    libraryDependencies ++= Seq(
      pureConfigCore,
      pureConfigCats,
      cats,
      catsEffect,
      fs2,
      logback % Runtime,
      log4cats,
      circeCore,
      circeGeneric,
      circeParser,
      http4sCore,
      http4sDsl,
      http4sServer,
      http4sClient,
      http4sCirce,
      http4sJwtAuth,
      squants,
      redis4catsCore,
      redis4catsStream,
      scalaTest        % Test,
      catsEffectTest   % Test,
      mockitoCore      % Test,
      mockitoScalatest % Test,
      redisEmbedded    % Test
    )
  )
