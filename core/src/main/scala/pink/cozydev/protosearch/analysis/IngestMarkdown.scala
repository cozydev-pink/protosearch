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

import cats.syntax.all._
import laika.api.MarkupParser
import laika.api.Renderer
import laika.ast.Document
import laika.ast.DocumentCursor
import laika.ast.Header
import laika.ast.RewriteRules
import laika.ast.Section
import laika.ast.Text
import laika.format.Markdown
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.markup.DocumentParser.ParserError
import laika.parse.markup.DocumentParser.RendererError
import laika.rewrite.nav.SectionBuilder

case class SubDocument(anchor: Option[String], title: String, content: String)

object IngestMarkdown {

  val parser = MarkupParser.of(Markdown).using(GitHubFlavor, SyntaxHighlighting).build

  val astRenderer = Renderer.of(Plaintext).build

  /** Creates a `RewriteRules` for a `Document` using the `SectionBuilder`.
    * Without the `SectionBuilder` an unresolved `Document` has `Header` nodes
    * separate from the content they introduce, afterwards they are grouped together
    * in a `Section` node.
    *
    * @param doc
    * @return
    */
  def sectionBuilderRule(doc: Document): Either[ParserError, RewriteRules] =
    DocumentCursor(doc).flatMap(SectionBuilder).leftMap(ParserError(_, doc.path))

  def parseResolvedWithSections(input: String): Either[ParserError, Document] =
    for {
      doc <- parser.parse(input)
      rules <- sectionBuilderRule(doc)
      result <- doc.rewrite(rules).leftMap(ParserError(_, doc.path))
    } yield result

  def transform(input: String): Either[RendererError, List[SubDocument]] =
    parseResolvedWithSections(input)
      .leftMap(e => RendererError(e.message, e.path))
      .flatMap(d =>
        d.content.collect { case Section(Header(_, Seq(Text(header, _)), opt), content, _) =>
          val plaintext = content.traverse(b => astRenderer.render(b)).map(_.mkString("\n"))
          plaintext.map(pt => SubDocument(opt.id, header, pt))
        }.sequence
      )
}
