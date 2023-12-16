// ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Wunused:all")
// FIXME after proper CORS config (skipped -Xfatal-warnings)
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Wunused:all")

val scala3Version  = "3.3.1"
val myOrganization = "Dawid Łakomy"

val circeVersion               = "0.14.1"
val catsEffectVersion          = "3.5.2"
val http4sVersion              = "0.23.23"
val doobieVersion              = "1.0.0-RC4"
val pureConfigVersion          = "0.17.4"
val log4catsVersion            = "2.6.0"
val tsecVersion                = "0.5.0"
val scalaTestVersion           = "3.2.17"
val scalaTestCatsEffectVersion = "1.5.0"
val testContainerVersion       = "1.19.1"
val logbackVersion             = "1.4.11"
val slf4jVersion               = "2.0.9"
val javaMailVersion            = "1.6.2"

lazy val common = (crossProject(JSPlatform, JVMPlatform) in file("common"))
  .settings(
    name         := "common",
    scalaVersion := scala3Version,
    organization := myOrganization
  )


///////////////////////////////////////////////////////////////////////////////////////////////////////////
// Frontend
///////////////////////////////////////////////////////////////////////////////////////////////////////////

// could be updated, but I'm not good with frontend, so
// I prefer the versions that Daniel used in the course
val tyrianVersion = "0.6.1"
val fs2DomVersion = "0.1.0"
val laikaVersion  = "0.19.0"

lazy val app = (project in file("app"))
  .enablePlugins(ScalaJSPlugin)
  .settings(
    name         := "app",
    scalaVersion := scala3Version,
    organization := myOrganization,
    libraryDependencies ++= Seq(
      "io.indigoengine" %%% "tyrian-io"     % tyrianVersion,
      "com.armanbilge"  %%% "fs2-dom"       % fs2DomVersion,
      "org.planet42"    %%% "laika-core"    % laikaVersion,
      "io.circe"        %%% "circe-core"    % circeVersion,
      "io.circe"        %%% "circe-parser"  % circeVersion,
      "io.circe"        %%% "circe-generic" % circeVersion
    ),
    scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) },
    semanticdbEnabled := true,
    autoAPIMappings   := true
  )
  .dependsOn(common.js)


lazy val server = project
  .in(file("server"))
  .settings(
    name         := "server",
    scalaVersion := scala3Version,
    organization := myOrganization,
    libraryDependencies ++= Seq(
      "org.typelevel"         %% "cats-effect"                   % catsEffectVersion,
      "org.http4s"            %% "http4s-dsl"                    % http4sVersion,
      "org.http4s"            %% "http4s-ember-server"           % http4sVersion,
      "org.http4s"            %% "http4s-circe"                  % http4sVersion,
      "io.circe"              %% "circe-generic"                 % circeVersion,
      "io.circe"              %% "circe-fs2"                     % circeVersion,
      "org.tpolecat"          %% "doobie-core"                   % doobieVersion,
      "org.tpolecat"          %% "doobie-hikari"                 % doobieVersion,
      "org.tpolecat"          %% "doobie-postgres"               % doobieVersion,
      "org.tpolecat"          %% "doobie-scalatest"              % doobieVersion              % Test,
      "com.github.pureconfig" %% "pureconfig-core"               % pureConfigVersion,
      "org.typelevel"         %% "log4cats-slf4j"                % log4catsVersion,
      "org.slf4j"              % "slf4j-simple"                  % slf4jVersion,
      "io.github.jmcardon"    %% "tsec-http4s"                   % tsecVersion,
      "com.sun.mail"           % "javax.mail"                    % javaMailVersion,
      "org.typelevel"         %% "log4cats-noop"                 % log4catsVersion            % Test,
      "org.scalatest"         %% "scalatest"                     % scalaTestVersion           % Test,
      "org.typelevel"         %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers"     % "testcontainers"                % testContainerVersion       % Test,
      "org.testcontainers"     % "postgresql"                    % testContainerVersion       % Test,
      "ch.qos.logback"         % "logback-classic"               % logbackVersion             % Test
    ),
    Compile / mainClass  := Some("dlakomy.jobsboard.Application"),
    Compile / run / fork := true, // cats complained
    run / connectInput   := true
  )
  .dependsOn(common.jvm)
