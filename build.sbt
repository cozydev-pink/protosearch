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

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// publish website from this branch
ThisBuild / tlSitePublishBranch := Some("main")

ThisBuild / resolvers +=
  "SonaType Snapshots".at("https://s01.oss.sonatype.org/content/repositories/snapshots/")

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))
ThisBuild / tlJdkRelease := Some(11)

val Scala213 = "2.13.10"
val Scala3 = "3.2.2"
ThisBuild / crossScalaVersions := Seq(Scala213, Scala3)
ThisBuild / scalaVersion := Scala3 // the default Scala

val calicoV = "0.2.0-RC2"
val catsEffectV = "3.5.0-RC5"
val catsV = "2.9.0"
val circeFs2V = "0.14.1"
val circeV = "0.14.5"
val fs2V = "3.6.1"
val http4sDomV = "0.2.8"
val http4sV = "0.23.18"
val laikaV = "0.19.1"
val lucilleV = "0.0-ed3aa4f-SNAPSHOT"
val munitCatsEffectV = "2.0.0-M3"
val munitV = "1.0.0-M7"
val scalajsDomV = "2.4.0"
def scodecV(scalaV: String) = if (scalaV.startsWith("2.")) "1.11.10" else "2.2.1"

lazy val root = tlCrossRootProject.aggregate(core, web, searchdocs, searchdocsWeb)

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
      "co.fs2" %%% "fs2-core" % fs2V,
      "co.fs2" %%% "fs2-io" % fs2V,
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "pink.cozydev" %%% "lucille" % lucilleV,
      "org.planet42" %%% "laika-core" % laikaV,
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
    ),
  )

lazy val searchdocs = project
  .in(file("searchdocs"))
  .enablePlugins(ScalaJSPlugin, NoPublishPlugin)
  .dependsOn(core.js)
  .settings(
    name := "protosearch-searchdocs",
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      import org.scalajs.linker.interface.ModuleSplitStyle
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("searchdocs")))
    },
    scalacOptions := scalacOptions.value
      .filterNot(_ == "-source:3.0-migration"),
    Compile / mainClass := Some("pink.cozydev.protosearch.DocumentationIndexWriterApp"),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("3."))
        Seq(
          "org.typelevel" %%% "cats-core" % catsV,
          "org.typelevel" %%% "cats-effect" % catsEffectV,
          "co.fs2" %%% "fs2-core" % fs2V,
          "co.fs2" %%% "fs2-io" % fs2V,
          "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
          "io.circe" %%% "circe-core" % circeV,
          "io.circe" %%% "circe-generic" % circeV,
          "io.circe" %%% "circe-parser" % circeV,
          "io.circe" %%% "circe-fs2" % circeFs2V,
        )
      else Seq()
    },
  )

lazy val searchdocsWeb = project
  .in(file("searchdocs-web"))
  .enablePlugins(ScalaJSPlugin, NoPublishPlugin)
  .dependsOn(core.js, searchdocs)
  .settings(
    name := "protosearch-searchdocs-web",
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      import org.scalajs.linker.interface.ModuleSplitStyle
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("searchdocs-web")))
    },
    scalacOptions := scalacOptions.value
      .filterNot(_ == "-source:3.0-migration"),
    Compile / mainClass := Some("pink.cozydev.protosearch.SearchDocs"),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("3."))
        Seq(
          "org.scala-js" %%% "scalajs-dom" % scalajsDomV,
          "org.typelevel" %%% "cats-core" % catsV,
          "org.typelevel" %%% "cats-effect" % catsEffectV,
          "co.fs2" %%% "fs2-core" % fs2V,
          "co.fs2" %%% "fs2-io" % fs2V,
          "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
          "pink.cozydev" %%% "lucille" % lucilleV,
          "com.armanbilge" %%% "calico" % calicoV,
          "io.circe" %%% "circe-core" % circeV,
          "io.circe" %%% "circe-generic" % circeV,
          "io.circe" %%% "circe-parser" % circeV,
          "io.circe" %%% "circe-fs2" % circeFs2V,
          "org.http4s" %%% "http4s-dom" % http4sDomV,
          "org.http4s" %%% "http4s-core" % http4sV,
          "org.http4s" %%% "http4s-circe" % http4sV,
          "org.http4s" %%% "http4s-dsl" % http4sV,
          "org.scalameta" %%% "munit" % munitV % Test,
          "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
        )
      else Seq()
    },
  )

lazy val web = project
  .in(file("web"))
  .enablePlugins(ScalaJSPlugin, NoPublishPlugin)
  .dependsOn(core.js)
  .settings(
    name := "protosearch-web",
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      import org.scalajs.linker.interface.ModuleSplitStyle
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("reposearch")))
    },
    scalacOptions := scalacOptions.value
      .filterNot(_ == "-source:3.0-migration"),
    Compile / mainClass := Some("pink.cozydev.protosearch.RepoSearch"),
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("3."))
        Seq(
          "org.scala-js" %%% "scalajs-dom" % scalajsDomV,
          "org.typelevel" %%% "cats-core" % catsV,
          "org.typelevel" %%% "cats-effect" % catsEffectV,
          "co.fs2" %%% "fs2-core" % fs2V,
          "co.fs2" %%% "fs2-io" % fs2V,
          "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
          "pink.cozydev" %%% "lucille" % lucilleV,
          "com.armanbilge" %%% "calico" % calicoV,
          "io.circe" %%% "circe-core" % circeV,
          "io.circe" %%% "circe-parser" % circeV,
          "io.circe" %%% "circe-fs2" % circeFs2V,
          "org.http4s" %%% "http4s-dom" % http4sDomV,
          "org.http4s" %%% "http4s-core" % http4sV,
          "org.http4s" %%% "http4s-circe" % http4sV,
          "org.http4s" %%% "http4s-dsl" % http4sV,
          "org.scalameta" %%% "munit" % munitV % Test,
          "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
        )
      else Seq()
    },
  )

import laika.helium.config.{IconLink, HeliumIcon}
lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(core.jvm, web, searchdocs, searchdocsWeb)
  .settings(
    tlSiteRelatedProjects := Seq(
      "lucene" -> url("https://lucene.apache.org/"),
      "lucille" -> url("https://github.com/cozydev-pink/lucille"),
      "textmogrify" -> url("https://github.com/valencik/textmogrify"),
    ),
    tlSiteHeliumConfig := {
      tlSiteHeliumConfig.value.site.topNavigationBar(
        homeLink = IconLink.external(
          "https://github.com/cozydev-pink/protosearch",
          HeliumIcon.github,
        )
      )
    },
    laikaInputs := {
      import laika.ast.Path.Root
      val jsArtifact = (web / Compile / fullOptJS / artifactPath).value
      val sourcemap = jsArtifact.getName + ".map"
      val jsArtifactDS = (searchdocsWeb / Compile / fullOptJS / artifactPath).value
      val sourcemapDS = jsArtifactDS.getName + ".map"
      laikaInputs.value.delegate
        .addFile(
          jsArtifact,
          Root / "reposearch" / "index.js",
        )
        .addFile(
          jsArtifact.toPath.resolveSibling(sourcemap).toFile,
          Root / "reposearch" / sourcemap,
        )
        .addFile(
          jsArtifactDS,
          Root / "searchdocs-web" / "index.js",
        )
        .addFile(
          jsArtifactDS.toPath.resolveSibling(sourcemapDS).toFile,
          Root / "searchdocs-web" / sourcemap,
        )
    },
    laikaSite := laikaSite
      .dependsOn(web / Compile / fullOptJS, searchdocsWeb / Compile / fullOptJS)
      .value,
  )
