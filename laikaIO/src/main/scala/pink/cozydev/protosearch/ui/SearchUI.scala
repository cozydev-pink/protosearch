/*
 * Copyright 2022 CozyDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pink.cozydev.protosearch.ui

import cats.effect.{Async, Resource}
import laika.ast.Path
import laika.io.model.InputTree
import laika.theme.{Theme, ThemeBuilder, ThemeProvider}
import laika.helium.Helium

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
      .addClassLoaderResource(
        s"$path/search.css",
        Path.Root / "search" / "search.css",
      )
      .addClassLoaderResource(
        s"$path/topNav.template.html",
        Path.Root / "helium" / "templates" / "topNav.template.html",
      )

    ThemeBuilder[F]("protosearch UI").addInputs(inputs).build
  }

  // Make a Helium => Helium function
  // so we can leverage: .extendWith(Helium => Helium)
  def searchNavBar(helium: Helium): Helium =
    helium.site.internalCSS(Path.Root / "search" / "search.css")
}
