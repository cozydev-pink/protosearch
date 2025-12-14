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
import laika.io.model.{InputTree, InputTreeBuilder}
import laika.theme.{Theme, ThemeBuilder, ThemeProvider}
import laika.helium.Helium
import laika.api.config.ConfigBuilder
import laika.config.LaikaKeys

object SearchUI extends ThemeProvider {

  private val path = "pink/cozydev/protosearch/sbt"

  /** Returns the path where the search index should be written.
    *
    * @param outputDir the site output directory (e.g. "target/site")
    * @return the full path for the search index file
    */
  def indexPath(outputDir: String): String = s"$outputDir/search/searchIndex.idx"

  private def baseInputs[F[_]: Async]: InputTreeBuilder[F] = {
    val unversioned = ConfigBuilder.empty.withValue(LaikaKeys.versioned, false).build

    InputTree[F]
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
        s"$path/searchBar.js",
        Path.Root / "search" / "searchBar.js",
      )
      .addClassLoaderResource(
        s"$path/search.html",
        Path.Root / "search" / "search.html",
      )
      .addClassLoaderResource(
        s"$path/search.css",
        Path.Root / "search" / "search.css",
      )
      .addConfig(unversioned, Path.Root / "search")
  }

  /** Theme provider for standalone use without Helium.
    *
    * Includes only the core search assets (JavaScript, CSS, HTML).
    * You'll need to add the search bar and modal to your own templates.
    */
  val standalone: ThemeProvider = new ThemeProvider {
    def build[F[_]: Async]: Resource[F, Theme[F]] =
      ThemeBuilder[F]("protosearch standalone").addInputs(baseInputs[F]).build
  }

  def build[F[_]: Async]: Resource[F, Theme[F]] = helium.build

  /** Theme provider for use with Helium.
    *
    * Includes all search assets plus the Helium-specific topNav template
    * and CSS that maps --ps-* vars to Helium theme vars.
    * Use with `.extendWith(SearchUI.helium)`.
    */
  val helium: ThemeProvider = new ThemeProvider {
    def build[F[_]: Async]: Resource[F, Theme[F]] = {
      val inputs = baseInputs[F]
        .addClassLoaderResource(
          s"$path/topNav.template.html",
          Path.Root / "helium" / "templates" / "topNav.template.html",
        )
        .addClassLoaderResource(
          s"$path/search-helium.css",
          Path.Root / "search" / "search-helium.css",
        )
      ThemeBuilder[F]("protosearch UI").addInputs(inputs).build
    }
  }

  // Make a Helium => Helium function
  // so we can leverage: .extendWith(Helium => Helium)
  def searchNavBar(helium: Helium): Helium =
    helium.site
      .internalCSS(Path.Root / "search" / "search.css")
      .site
      .internalCSS(Path.Root / "search" / "search-helium.css")
      .site
      .internalJS(Path.Root / "search" / "protosearch.js")
      .site
      .internalJS(Path.Root / "search" / "searchBar.js")
}
