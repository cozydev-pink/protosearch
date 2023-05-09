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

package pink.cozydev.protosearch.analysis

import cats.data.NonEmptyList
import cats.effect.IO
import laika.api.MarkupParser
import laika.config.LaikaKeys
import laika.format.Markdown
import laika.io.implicits._
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.markup.DocumentParser.RendererError

object DocsDirectory {

  val mdParser = MarkupParser
    .of(Markdown)
    .using(GitHubFlavor, SyntaxHighlighting)
    .withConfigValue(LaikaKeys.validateLinks, false)
    .parallel[IO]
    .build

  def dirToDocs(dirPath: String): IO[Seq[Either[RendererError, NonEmptyList[SubDocument]]]] =
    mdParser.use(parser =>
      parser.fromDirectory(dirPath).parse.map { tree =>
        tree.root.allDocuments.map(IngestMarkdown.renderSubDocuments)
      }
    )
}
