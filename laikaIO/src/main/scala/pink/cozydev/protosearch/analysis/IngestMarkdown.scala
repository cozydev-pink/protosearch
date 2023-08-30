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
import cats.syntax.all._
import laika.api.MarkupParser
import laika.api.Renderer
import laika.ast.Block
import laika.ast.Document
import laika.ast.Header
import laika.ast.RootElement
import laika.ast.Section
import laika.ast.Text
import laika.format.Markdown
import laika.markdown.github.GitHubFlavor
import laika.parse.code.SyntaxHighlighting
import laika.parse.markup.DocumentParser.RendererError

case class SubDocument(fileName: String, anchor: Option[String], title: String, content: String)

object IngestMarkdown {

  val parser = MarkupParser.of(Markdown).using(GitHubFlavor, SyntaxHighlighting).build

  val astRenderer = Renderer.of(Plaintext).build

  private def renderSeqBlock(bs: Seq[Block]): Either[RendererError, String] =
    bs.toList.traverse(b => astRenderer.render(b)).map(_.mkString("\n"))

  /** Collects blocks from a RootElement until the predicate is satisfied, does not
    * include the block that satisfies the predicate.
    *
    * @param root The root element
    * @param split the predicate to stop collecting blocks
    * @return
    */
  private def groupUntil(root: RootElement)(split: Block => Boolean): Option[List[Block]] = {
    val blocks = root.content.takeWhile(!split(_)).toList
    if (blocks.nonEmpty) Some(blocks) else None
  }

  def renderSubDocuments(doc: Document): Either[RendererError, NonEmptyList[SubDocument]] = {
    val fileName = doc.path.name
    // Group content before first section
    val preamble = groupUntil(doc.content)(b => b.isInstanceOf[Section])
      .map(bs => renderSeqBlock(bs).map(pt => SubDocument(fileName, None, doc.path.basename, pt)))

    // Collect all sections into subdocs
    val sectionDocs: List[Either[RendererError, SubDocument]] = doc.content.collect {
      case Section(Header(_, Seq(Text(header, _)), opt), content, _) =>
        renderSeqBlock(content).map(pt => SubDocument(fileName, opt.id, header, pt))
    }

    // Combine subdocs
    val both: List[Either[RendererError, SubDocument]] = preamble match {
      case None => sectionDocs
      case Some(pre) => pre :: sectionDocs
    }

    // groupUntil might handle this case
    val subDocs: NonEmptyList[Either[RendererError, SubDocument]] =
      NonEmptyList.fromList(both) match {
        case Some(subdocs) => subdocs
        case None =>
          // One last attempt at the whole doc
          NonEmptyList.one(
            renderSeqBlock(doc.content.content).map(pt =>
              SubDocument(fileName, doc.content.options.id, doc.path.basename, pt)
            )
          )
      }
    subDocs.sequence
  }

  def transform(input: String): Either[RendererError, NonEmptyList[SubDocument]] =
    parser
      .parse(input)
      .leftMap(e => RendererError(e.message, e.path))
      .flatMap(renderSubDocuments)

}
