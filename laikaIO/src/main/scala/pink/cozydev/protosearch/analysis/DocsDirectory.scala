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

import cats.effect.IO
import laika.api.MarkupParser
import laika.format.Markdown
import laika.format.Markdown.GitHubFlavor
import laika.config.SyntaxHighlighting
import laika.io.syntax._
import laika.api.Renderer
import cats.effect.IOApp
import cats.effect.kernel.Resource
import laika.api.errors.RendererError
import cats.data.NonEmptyList

object DocsDirectory extends IOApp.Simple {

  val parserBuilder = MarkupParser
    .of(Markdown)
    .using(GitHubFlavor, SyntaxHighlighting)

  val parser = parserBuilder
    .parallel[IO]
    .build

  val config = parserBuilder.config

  val plaintextRenderer = Renderer.of(Plaintext).withConfig(config).parallel[IO].build

  val run = Resource
    .both(parser, plaintextRenderer)
    .use((parser, renderer) =>
      parser
        .fromDirectory("/home/andrew/src/github.com/cozydev/protosearch/docs")
        .parse
        .flatMap(tree => renderer.from(tree.root).toDirectory("outputDir").render.void)
    )

  def dirToDocs(dirPath: String): IO[Seq[Either[RendererError, NonEmptyList[SubDocument]]]] =
    parser.use(parser =>
      parser.fromDirectory(dirPath).parse.map { tree =>
        tree.root.allDocuments.map(IngestMarkdown.renderSubDocuments)
      }
    )
}
