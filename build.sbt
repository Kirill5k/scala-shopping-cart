import Dependencies._

ThisBuild / scalaVersion := "2.13.1"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "io.kirill"
ThisBuild / organizationName := "kirill"

lazy val root = (project in file("."))
  .settings(
    name := "scala-shopping-cart",
    libraryDependencies ++= Seq(
      pureConfig,
      pureConfigCats,
      cats,
      catsEffect,
      fs2,
      logback,
      log4cats,
      circe,
      circeGeneric,
      circeParser,
      http4s,
      http4sDsl,
      http4sServer,
      http4sBlaze,
      http4sCirce,
      scalaTest      % Test,
      catsEffectTest % Test
    )
  )
