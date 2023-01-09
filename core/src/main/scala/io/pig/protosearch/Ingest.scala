package io.pig.protosearch

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

object Ingest {

  val parser = MarkupParser.of(Markdown).using(GitHubFlavor, SyntaxHighlighting).build

  val astRenderer = Renderer.of(AST).build
  val htmlRenderer = Renderer.of(HTML).build

  /** Creates a [[RewriteRules]] for a [[Document]] using the [[SectionBuilder]].
    * Without the [[SectionBuilder]] an unresolved [[Document]] has [[Header]] nodes
    * separate from the content they introduce, afterwards they are grouped together
    * in a [[Section]] node.
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
          header
        }
      )
}
