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

val Scala212 = "2.12.18"
val Scala213 = "2.13.11"
val Scala3 = "3.3.0"
ThisBuild / crossScalaVersions := Seq(Scala212, Scala213, Scala3)
ThisBuild / scalaVersion := Scala3 // the default Scala

val calicoV = "0.2.1"
val catsEffectV = "3.5.1"
val catsV = "2.10.0"
val circeFs2V = "0.14.1"
val circeV = "0.14.5"
val fs2V = "3.9.0"
val http4sDomV = "0.2.9"
val http4sV = "0.23.23"
val laikaV = "0.19.3"
val lucilleV = "0.0-b9ba3fa-SNAPSHOT"
val munitCatsEffectV = "2.0.0-M3"
val munitV = "1.0.0-M8"
val scalajsDomV = "2.6.0"
def scodecV(scalaV: String) = if (scalaV.startsWith("2.")) "1.11.10" else "2.2.1"

lazy val root =
  tlCrossRootProject.aggregate(
    core,
    laikaIO,
    jsInterop,
    web,
    searchdocsCore,
    searchdocsIO,
    searchdocsWeb,
  )

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
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
    ),
  )

lazy val laikaIO = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("laikaIO"))
  .settings(
    name := "protosearch-laika",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "co.fs2" %%% "fs2-core" % fs2V,
      "co.fs2" %%% "fs2-io" % fs2V,
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "pink.cozydev" %%% "lucille" % lucilleV,
      "org.planet42" %%% "laika-core" % laikaV,
      "org.planet42" %%% "laika-io" % laikaV,
      "org.scalameta" %%% "munit" % munitV % Test,
      "org.typelevel" %%% "munit-cats-effect" % munitCatsEffectV % Test,
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

lazy val searchdocsCore = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("searchdocs"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core)
  .jsSettings(
    scalaJSUseMainModuleInitializer := true,
    Compile / fastLinkJS / scalaJSLinkerConfig ~= {
      import org.scalajs.linker.interface.ModuleSplitStyle
      _.withModuleKind(ModuleKind.ESModule)
        .withModuleSplitStyle(ModuleSplitStyle.SmallModulesFor(List("searchdocs")))
    },
  )
  .settings(
    name := "protosearch-searchdocs",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "co.fs2" %%% "fs2-core" % fs2V,
      "io.circe" %%% "circe-core" % circeV,
      "io.circe" %%% "circe-generic" % circeV,
      "io.circe" %%% "circe-parser" % circeV,
      "io.circe" %%% "circe-fs2" % circeFs2V,
    ),
  )

lazy val searchdocsIO = crossProject(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("searchdocs-io"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, laikaIO, searchdocsCore)
  .settings(
    name := "protosearch-searchdocs-io",
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "cats-core" % catsV,
      "org.typelevel" %%% "cats-effect" % catsEffectV,
      "co.fs2" %%% "fs2-core" % fs2V,
      "co.fs2" %%% "fs2-io" % fs2V,
      "org.scodec" %%% "scodec-core" % scodecV(scalaVersion.value),
      "io.circe" %%% "circe-core" % circeV,
      "io.circe" %%% "circe-generic" % circeV,
      "io.circe" %%% "circe-parser" % circeV,
      "io.circe" %%% "circe-fs2" % circeFs2V,
    ),
  )

lazy val searchdocsWeb = project
  .in(file("searchdocs-web"))
  .enablePlugins(ScalaJSPlugin, NoPublishPlugin)
  .dependsOn(core.js, searchdocsCore.js)
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
  .dependsOn(core.jvm, web, searchdocsCore.js, searchdocsWeb, jsInterop.js)
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
        .mainNavigation(appendLinks =
          Seq(
            ThemeNavigationSection(
              "Related Projects",
              TextLink.external("https://lucene.apache.org/", "lucene"),
              TextLink.external("https://github.com/cozydev-pink/lucille", "lucille"),
              TextLink.external("https://github.com/valencik/textmogrify", "textmogrify"),
            )
          )
        )
    },
    laikaInputs := {
      import laika.ast.Path.Root
      import laika.io.model.FilePath
      val jsArtifact = (web / Compile / fullOptJS / artifactPath).value
      val sourcemap = jsArtifact.getName + ".map"
      val jsArtifactDS = (searchdocsWeb / Compile / fullOptJS / artifactPath).value
      val sourcemapDS = jsArtifactDS.getName + ".map"
      val jsArtifactInterop = (jsInterop.js / Compile / fullOptJS / artifactPath).value
      val sourcemapInterop = jsArtifactInterop.getName + ".map"
      laikaInputs.value.delegate
        .addFile(
          FilePath.fromJavaFile(jsArtifact),
          Root / "reposearch" / "index.js",
        )
        .addFile(
          FilePath.fromNioPath(jsArtifact.toPath.resolveSibling(sourcemap)),
          Root / "reposearch" / sourcemap,
        )
        .addFile(
          FilePath.fromJavaFile(jsArtifactDS),
          Root / "searchdocs" / "index.js",
        )
        .addFile(
          FilePath.fromNioPath(jsArtifactDS.toPath.resolveSibling(sourcemapDS)),
          Root / "searchdocs" / sourcemap,
        )
        .addFile(
          FilePath.fromJavaFile(jsArtifactInterop),
          Root / "interop" / "index.js",
        )
        .addFile(
          FilePath.fromNioPath(jsArtifactInterop.toPath.resolveSibling(sourcemapInterop)),
          Root / "interop" / sourcemap,
        )
    },
    laikaSite := laikaSite
      .dependsOn(
        web / Compile / fullOptJS,
        searchdocsWeb / Compile / fullOptJS,
        jsInterop.js / Compile / fullOptJS,
      )
      .value,
  )
