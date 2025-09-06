// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / organization := "pink.cozydev"
ThisBuild / organizationName := "CozyDev"
ThisBuild / startYear := Some(2022)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  // your GitHub handle and name
  tlGitHubDev("valencik", "Andrew Valencik"),
  tlGitHubDev("samspills", "Sam Pillsworth"),
)

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

ThisBuild / resolvers +=
  "SonaType Snapshots".at("https://s01.oss.sonatype.org/content/repositories/snapshots/")

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))
ThisBuild / tlJdkRelease := Some(11)

val Scala212 = "2.12.20"
val Scala213 = "2.13.16"
val Scala3 = "3.3.6"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
ThisBuild / scalaVersion := Scala212 // the default Scala

// Plugin setup stolen from Laika with love
ThisBuild / githubWorkflowBuildMatrixExclusions ++=
  List("2.13", "3").map(scala => MatrixExclude(Map("project" -> "plugin", "scala" -> scala)))
ThisBuild / githubWorkflowBuildMatrixAdditions ~= { matrix =>
  matrix + ("project" -> (matrix("project") :+ "plugin"))
}

val catsEffectV = "3.6.3"
val catsV = "2.13.0"
val fs2V = "3.12.2"
val laikaV = "1.3.2"
val lucilleV = "0.0.3"
val munitCatsEffectV = "2.1.0"
val munitV = "1.1.1"
val scalajsDomV = "2.8.1"
def scodecV(scalaV: String) = if (scalaV.startsWith("2.")) "1.11.10" else "2.3.2"
val scalametaV = "4.13.4"

lazy val root =
  tlCrossRootProject
    .aggregate(
      core,
      laikaIO,
      jsInterop,
    )
    .configureRoot { root =>
      root.aggregate(plugin, scaladoc) // don't include the plugin in rootJVM, only in root
    }

lazy val core = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
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
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "pink.cozydev" %%% "lucille" % lucilleV,
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
    ),
  )

lazy val laikaIO = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("laikaIO"))
  .dependsOn(core)
  .settings(
    name := "protosearch-laika",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "co.fs2" %%% "fs2-core" % fs2V,
      "co.fs2" %%% "fs2-io" % fs2V,
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "pink.cozydev" %%% "lucille" % lucilleV,
      "org.typelevel" %%% "laika-core" % laikaV,
      "org.typelevel" %%% "laika-io" % laikaV,
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
    ),
    Compile / packageBin / mappings += {
      val jsArtifactInterop = (jsInterop.js / Compile / fullOptJS).value.data
      val inDir = baseDirectory.value / "src" / "main" / "resources"
      val dir = "pink/cozydev/protosearch/sbt"
      jsArtifactInterop -> s"$dir/protosearch.js"
    },
  )

lazy val scaladoc = project
  .in(file("scaladoc"))
  .dependsOn(core.jvm)
  .settings(
    name := "protosearch-scaladoc",
    crossScalaVersions := Seq(Scala212),
    libraryDependencies ++= Seq(
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.scalameta" %%% "scalameta" % scalametaV,
    ),
  )

lazy val jsInterop = crossProject(JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("jsinterop"))
  .dependsOn(core)
  .settings(
    name := "protosearch-jsinterop",
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % scalajsDomV
    ),
  )

lazy val plugin =
  project
    .in(file("sbt"))
    .dependsOn(core.jvm, laikaIO.jvm, scaladoc)
    .enablePlugins(SbtPlugin)
    .settings(
      name := "protosearch-sbt",
      sbtPlugin := true,
      crossScalaVersions := Seq(Scala212),
      addSbtPlugin("org.typelevel" % "laika-sbt" % laikaV),
      addSbtPlugin("org.typelevel" % "sbt-typelevel-site" % "0.8.0"),
      Compile / packageBin / mappings += {
        val jsArtifactInterop = (jsInterop.js / Compile / fullOptJS).value.data
        val inDir = baseDirectory.value / "src" / "main" / "resources"
        val dir = "pink/cozydev/protosearch/sbt"
        jsArtifactInterop -> s"$dir/protosearch.js"
      },
    )

import laika.helium.config.{IconLink, HeliumIcon}
lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm, jsInterop.js)
  .settings(
    tlSiteHelium ~= {
      import laika.helium.config._
      import laika.ast.Path.Root
      _.site
        .topNavigationBar(
          // override default because our README.md is a symlink
          homeLink = IconLink.internal(Root / "index.md", HeliumIcon.home)
        )
        .site
        .pageNavigation(keepOnSmallScreens = true)
        .site
        .mainNavigation(appendLinks =
          Seq(
            ThemeNavigationSection(
              "Related Projects",
              TextLink.external("https://lucene.apache.org/", "lucene"),
              TextLink.external("https://github.com/cozydev-pink/lucille", "lucille"),
              TextLink.external("https://github.com/cozydev-pink/textmogrify", "textmogrify"),
            )
          )
        )
    },
    laikaInputs := {
      import laika.ast.Path.Root
      import laika.io.model.FilePath
      val jsArtifactInterop = (jsInterop.js / Compile / fullOptJS / artifactPath).value
      val sourcemapInterop = jsArtifactInterop.getName + ".map"
      laikaInputs.value.delegate
        .addFile(
          FilePath.fromJavaFile(jsArtifactInterop),
          Root / "interop" / "index.js",
        )
        .addFile(
          FilePath.fromNioPath(jsArtifactInterop.toPath.resolveSibling(sourcemapInterop)),
          Root / "interop" / sourcemapInterop,
        )
    },
    laikaSite := laikaSite
      .dependsOn(
        jsInterop.js / Compile / fullOptJS
      )
      .value,
  )
