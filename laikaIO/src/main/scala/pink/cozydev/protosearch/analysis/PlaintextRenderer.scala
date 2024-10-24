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

import laika.ast.*
import laika.api.format.{Formatter, RenderFormat}
import laika.ast.html.HTMLSpan

object PlaintextRenderer extends ((Formatter, Element) => String) {

  def apply(fmt: Formatter, element: Element): String = {

    def renderElement(e: Element): String = {
      val (elements, _) = e.productIterator.partition(_.isInstanceOf[Element])
      fmt.children(
        elements.toList.asInstanceOf[Seq[Element]]
      )
    }

    def renderListContainer(con: ListContainer): String = con match {
      /* Excluded as they would either produce unwanted entries (e.g. the headline of a different page)
       * or duplicate entries (e.g. a section title on the current page)
       */
      case _: NavigationList | _: NavigationItem => ""
      case _ => fmt.children(con.content)
    }

    def renderBlockContainer(con: BlockContainer): String = con match {
      /* Some special handling for the few containers which hold child nodes
         in more properties than just the container's `content` property.
       */
      case Section(header, content, _) => renderBlock(header.content) + renderBlocks(content)
      case QuotedBlock(content, attr, _) => renderBlocks(content) + renderBlock(attr)
      case TitledBlock(title, content, _) => renderBlock(title) + renderBlocks(content)
      case Figure(_, caption, content, _) => renderBlock(caption) + renderBlocks(content)
      case DefinitionListItem(term, defn, _) => renderBlock(term) + renderBlocks(defn)
      case _ => renderBlocks(con.content)
    }

    def renderElementContainer(con: ElementContainer[? <: Element]): String = con match {
      /* SectionInfo is solely used in navigation structures and represents duplicate info.
       */
      case _: SectionInfo => ""
      /* All other core AST types implement one of the sub-traits of ElementContainer -
         if we end up here it's an unknown 3rd party node
       */
      case _ => fmt.children(con.content)
    }

    def renderTextContainer(con: TextContainer): String = con match {
      /* match on most common container type first for performance */
      case Text(content, _) => content
      /* could be any unknown markup format */
      case _: RawContent => ""
      /* comments are usually ignored by search engines */
      case _: Comment => ""
      /* embedded debug info node */
      case _: RuntimeMessage => ""
      /* this does not represent text nodes in verbatim HTML */
      case _: HTMLSpan => ""
      case _: SectionNumber => ""
      case _ => con.content
    }

    def renderTable(table: Table): String = {
      val cells = (table.head.content ++ table.body.content).flatMap(_.content)
      renderBlocks(cells.flatMap(_.content)) + renderBlock(table.caption.content)
    }

    def renderBlocks(blocks: Seq[Block]): String =
      if (blocks.nonEmpty) fmt.childPerLine(blocks) + fmt.newLine
      else ""

    def renderBlock(spans: Seq[Span]): String =
      if (spans.nonEmpty) fmt.children(spans) + fmt.newLine
      else ""

    element match {
      /* These are marker traits for nodes we should ignore.
       * They usually also implement some of the other traits we match on,
       * so this always needs to come first. */
      case _: Hidden | _: Unresolved | _: Invalid => ""
      case lc: ListContainer => renderListContainer(lc)
      case bc: BlockContainer => renderBlockContainer(bc)
      case sc: SpanContainer => fmt.children(sc.content)
      case ec: ElementContainer[?] => renderElementContainer(ec)
      case tc: TextContainer => renderTextContainer(tc)
      case _: HTMLSpan => ""
      case t: Table => renderTable(t)
      case e => renderElement(e)
    }
  }
}

case object Plaintext extends RenderFormat[Formatter] {
  val fileSuffix = "txt"

  val defaultRenderer: (Formatter, Element) => String =
    PlaintextRenderer

  val formatterFactory: Formatter.Context[Formatter] => Formatter =
    context => Formatter.defaultFactory(context.withIndentation(Formatter.Indentation.default))
}
