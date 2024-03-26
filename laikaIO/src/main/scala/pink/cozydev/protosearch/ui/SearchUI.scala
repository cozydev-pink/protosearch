package pink.cozydev.protosearch.ui

import cats.effect.{Async, Resource}
import laika.ast.Path
import laika.io.model.InputTree
import laika.theme.{Theme, ThemeBuilder, ThemeProvider}

object SearchUI extends ThemeProvider {

  def build[F[_]: Async]: Resource[F, Theme[F]] = {

    val path = "pink/cozydev/protosearch/sbt"

    val inputs = InputTree[F]
      .addClassLoaderResource(
        s"$path/protosearch.js",
        Path.Root / "search" / "protosearch.js",
      )
      .addClassLoaderResource(
        s"$path/search.js",
        Path.Root / "search" / "search.js",
      )
      .addClassLoaderResource(
        s"$path/worker.js",
        Path.Root / "search" / "worker.js",
      )
      .addClassLoaderResource(
        s"$path/search.html",
        Path.Root / "search" / "search.html",
      )

    ThemeBuilder[F]("protosearch UI").addInputs(inputs).build
  }
}
