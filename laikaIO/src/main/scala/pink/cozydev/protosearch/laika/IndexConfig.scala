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

package pink.cozydev.protosearch.laika

import laika.io.config.BinaryRendererConfig
import laika.io.config.Artifact
import laika.ast.Path

final case class IndexConfigBuilder(
  excludedPaths: List[Path],
  renderWithLaikaSiteCommand: Boolean
) {
  /* Exclude the provided paths from the index */
  def withExcludedPaths(paths: Path*): IndexConfigBuilder =
    copy(excludedPaths = paths.toList)

  /* Build the `IndexFormat` for this config */
  def format: IndexFormat =
    IndexFormat(excludePaths = excludedPaths)

  /* Build the `BinaryRendererConfig` for this config */
  def config: BinaryRendererConfig =
    BinaryRendererConfig(
      alias = "index",
      format = format,
      artifact = Artifact(
        basePath = Path.Root / "search" / "searchIndex",
        suffix = "idx"
      ),
      includeInSite = renderWithLaikaSiteCommand,
      supportsSeparations = false
    )
}
object IndexConfig {

  /* A configuration for indexing all docs in the site in protosearch. */
  val default: IndexConfigBuilder = IndexConfigBuilder(Nil, true)

  /* A configuration for indexing docs with some exclusions. */
  def withExcludedPaths(paths: Path*): IndexConfigBuilder =
    IndexConfigBuilder(paths.toList, true)
}
