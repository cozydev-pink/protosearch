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

import laika.ast._
import laika.api.format.{Formatter, RenderFormat}

object PlaintextRenderer extends ((Formatter, Element) => String) {

  def apply(fmt: Formatter, element: Element): String = {

    def renderElement(e: Element): String = {
      val (elements, _) = e.productIterator.partition(_.isInstanceOf[Element])
      e.productPrefix + fmt.indentedChildren(
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

    def renderBlocks(blocks: Seq[Block]): String =
      if (blocks.nonEmpty) fmt.childPerLine(blocks) + fmt.newLine
      else ""

    def renderBlock(spans: Seq[Span]): String =
      if (spans.nonEmpty) fmt.children(spans) + fmt.newLine
      else ""

    element match {
      case lc: ListContainer => renderListContainer(lc)
      case bc: BlockContainer => renderBlockContainer(bc)
      case _: SectionNumber => ""
      case tc: TextContainer => tc.content
      case ec: ElementContainer[_] => fmt.children(ec.content)
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
