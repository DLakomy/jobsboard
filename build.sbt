scalacOptions ++= Seq("-unchecked", "-deprecation", "-feature", "-Xfatal-warnings", "-Wunused:all")

val scala3Version = "3.3.1"

val circeVersion = "0.14.1"
val catsEffectVersion = "3.5.2"
val http4sVersion = "0.23.23"
val doobieVersion = "1.0.0-RC4"
val pureConfigVersion = "0.17.4"
val log4catsVersion = "2.6.0"
val tsecVersion = "0.5.0"
val scalaTestVersion = "3.2.17"
val scalaTestCatsEffectVersion = "1.5.0"
val testContainerVersion = "1.19.1"
val logbackVersion = "1.4.11"
val slf4jVersion = "2.0.9"
val javaMailVersion = "1.6.2"

// cats complained
Compile / run / fork := true

lazy val root = project
  .in(file("."))
  .settings(
    name := "jobsboard",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-effect" % catsEffectVersion,
      "org.http4s" %% "http4s-dsl" % http4sVersion,
      "org.http4s" %% "http4s-ember-server" % http4sVersion,
      "org.http4s" %% "http4s-circe" % http4sVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-fs2" % circeVersion,
      "org.tpolecat" %% "doobie-core" % doobieVersion,
      "org.tpolecat" %% "doobie-hikari" % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      "org.tpolecat" %% "doobie-scalatest" % doobieVersion % Test,
      "com.github.pureconfig" %% "pureconfig-core" % pureConfigVersion,
      "org.typelevel" %% "log4cats-slf4j" % log4catsVersion,
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "io.github.jmcardon" %% "tsec-http4s" % tsecVersion,
      "com.sun.mail" % "javax.mail" % javaMailVersion,
      "org.typelevel" %% "log4cats-noop" % log4catsVersion % Test,
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
      "org.typelevel" %% "cats-effect-testing-scalatest" % scalaTestCatsEffectVersion % Test,
      "org.testcontainers" % "testcontainers" % testContainerVersion % Test,
      "org.testcontainers" % "postgresql" % testContainerVersion % Test,
      "ch.qos.logback" % "logback-classic" % logbackVersion % Test
    )
  )
