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
import laika.render.TextFormatter
import laika.factory.RenderFormat
import laika.factory.RenderContext
import laika.render.Indentation

object PlaintextRenderer extends ((TextFormatter, Element) => String) {

  private case class Content(content: Seq[Element], options: Options = NoOpt)
      extends Element
      with ElementContainer[Element] {
    type Self = Content
    def withOptions(options: Options): Content = copy(options = options)
  }

  def apply(fmt: TextFormatter, element: Element): String = {

    def renderElement(e: Element): String = {
      val (elements, _) = e.productIterator.partition(_.isInstanceOf[Element])
      e.productPrefix + fmt.indentedChildren(
        elements.toList.asInstanceOf[Seq[Element]]
      )
    }

    def lists(lists: Seq[Element]*): String =
      fmt.childPerLine(lists.map { case elems => Content(elems) })

    element match {
      case _: Section => "" // short circuit rendering
      case _: SectionNumber => ""
      case QuotedBlock(content, attr, _) => lists(content, attr)
      case DefinitionListItem(term, defn, _) => lists(term, defn)
      case tc: TextContainer => tc.content
      case Content(content, _) => fmt.indentedChildren(content)
      case ec: ElementContainer[_] => fmt.children(ec.content)
      case e => renderElement(e)
    }
  }
}

case object Plaintext extends RenderFormat[TextFormatter] {
  val fileSuffix = "txt"

  val defaultRenderer: (TextFormatter, Element) => String = PlaintextRenderer

  val formatterFactory: RenderContext[TextFormatter] => TextFormatter = PlaintextFormatter
}

object PlaintextFormatter extends (RenderContext[TextFormatter] => TextFormatter) {
  def apply(context: RenderContext[TextFormatter]): TextFormatter =
    TextFormatter(context.renderChild, context.root, Nil, Indentation.default)
}
