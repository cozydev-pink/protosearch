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

    def renderBlocks(blocks: Seq[Block]): String =
      if (blocks.nonEmpty) fmt.childPerLine(blocks) + fmt.newLine
      else ""

    def renderBlock(spans: Seq[Span]): String =
      if (spans.nonEmpty) fmt.children(spans) + fmt.newLine
      else ""

    element match {
      case s: Section => renderBlock(s.header.content) + renderBlocks(s.content)
      case _: SectionNumber => ""
      case QuotedBlock(content, attr, _) => renderBlocks(content) + renderBlock(attr)
      case DefinitionListItem(term, defn, _) => renderBlock(term) + renderBlocks(defn)
      case bc: BlockContainer => renderBlocks(bc.content)
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
