// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "pink.cozydev"
ThisBuild / organizationName := "CozyDev"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("valencik", "Andrew Valencik")
)

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// do not publish to sonatype yet
ThisBuild / tlCiReleaseBranches := Seq.empty

ThisBuild / resolvers +=
  "SonaType Snapshots".at("https://s01.oss.sonatype.org/content/repositories/snapshots/")

val Scala213 = "2.13.10"
ThisBuild / crossScalaVersions := Seq(Scala213, "3.2.2")
ThisBuild / scalaVersion := Scala213 // the default Scala

val catsV = "2.9.0"
val catsEffectV = "3.4.7"
val fs2V = "3.6.1"
val laikaV = "0.19.0"
val lucilleV = "0.0-afc5220-SNAPSHOT"
def scodecV(scalaV: String) = if (scalaV.startsWith("2.")) "1.11.10" else "2.2.1"
val munitV = "1.0.0-M6"
val munitCatsEffectV = "2.0.0-M3"

lazy val root = tlCrossRootProject.aggregate(core)

lazy val core = project
  .in(file("core"))
  .settings(
    name := "protosearch",
    Compile / run / fork := true,
    // forward stdin to forked process
    Compile / run / connectInput := true,
    // send forked output to stdout
    outputStrategy := Some(StdoutOutput),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "co.fs2" %%% "fs2-core" % fs2V,
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "pink.cozydev" %%% "lucille" % lucilleV,
      "org.planet42" %%% "laika-core" % laikaV,
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
    ),
  )
