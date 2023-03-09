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
import laika.format.Markdown
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.api.Renderer
import laika.format.AST
import laika.parse.markup.DocumentParser.ParserError
import laika.format.HTML
import laika.ast.RewriteRules
import laika.ast.DocumentCursor
import laika.rewrite.nav.SectionBuilder
import laika.ast.Document
import laika.ast.Header
import laika.ast.Text
import laika.ast.Section

// Similar to the other misfits in `analysis` this shouldn't live here
object IngestMarkdown {

  val parser = MarkupParser.of(Markdown).using(GitHubFlavor, SyntaxHighlighting).build

  val astRenderer = Renderer.of(AST).build
  val htmlRenderer = Renderer.of(HTML).build

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

  def parseWithSections(input: String): Either[ParserError, Document] =
    for {
      doc <- parser.parseUnresolved(input).map(_.document)
      rules <- sectionBuilderRule(doc)
      result <- doc.rewrite(rules).leftMap(ParserError(_, doc.path))
    } yield result

  def transform(input: String): Either[ParserError, List[String]] =
    parseWithSections(input)
      .map(d =>
        d.content.collect { case Section(Header(_, Seq(Text(header, _)), _), content, _) =>
          // build a Document with just this header + content
          // don't worry about plaintext rendering here
          // need to track position offsets
          // perhaps need custom tokenizer
          (header :: content :: Nil).mkString
        }
      )
}
