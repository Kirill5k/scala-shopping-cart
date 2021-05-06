import com.typesafe.sbt.packager.docker.{Cmd, DockerStageBreak}

ThisBuild / scalaVersion := "2.13.5"
ThisBuild / version := scala.sys.process.Process("git rev-parse HEAD").!!.trim.slice(0, 7)
ThisBuild / organization := "io.github.kirill5k"

lazy val noPublish = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false,
  publish / skip := true
)

lazy val docker = Seq(
  packageName := moduleName.value,
  version := version.value,
  maintainer := "immotional@aol.com",
  dockerBaseImage := "adoptopenjdk/openjdk16-openj9:x86_64-alpine-jre-16_36_openj9-0.25.0",
  dockerUpdateLatest := true,
  makeBatScripts := List(),
  dockerRepository := Some("us.gcr.io"),
  dockerCommands := {
    val commands         = dockerCommands.value
    val (stage0, stage1) = commands.span(_ != DockerStageBreak)
    val (before, after)  = stage1.splitAt(4)
    val installBash      = Cmd("RUN", "apk update && apk upgrade && apk add bash && apk add curl")
    stage0 ++ before ++ List(installBash) ++ after
  }
)

lazy val root = project
  .in(file("."))
  .settings(noPublish)
  .settings(
    name := "shopping-cart"
  )
  .aggregate(core)

lazy val core = project
  .in(file("modules/core"))
  .enablePlugins(DockerPlugin, AshScriptPlugin)
  .settings(docker)
  .settings(
    name := "shopping-cart-core",
    scalacOptions += "-Ymacro-annotations",
    scalafmtOnCompile := true,
    resolvers += Resolver.sonatypeRepo("snapshots"),
    Defaults.itSettings,
    Docker / packageName := "shopping-cart/core",
    libraryDependencies ++= Dependencies.core ++ Dependencies.test,
    libraryDependencies += compilerPlugin("org.typelevel" %% "kind-projector" % "0.11.3" cross CrossVersion.full)
  )
