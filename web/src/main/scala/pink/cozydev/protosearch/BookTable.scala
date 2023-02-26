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

package pink.cozydev.protosearch

import org.scalajs.dom
import org.scalajs.dom.document

object BookTable {
  import BookIndex.{corpus, Book}

  def renderBookRow(targetNode: dom.Node, book: Book): Unit = {
    val row = dom.document.createElement("tr")
    row.appendChild {
      val td = dom.document.createElement("td")
      td.innerText = book.author
      td
    }
    row.appendChild {
      val td = dom.document.createElement("td")
      td.innerText = book.title
      td
    }
    targetNode.appendChild(row)
    ()
  }

  def renderBookTable(targetNode: dom.Node): Unit = {
    val table = document.createElement("table")
    table.appendChild {
      val td = dom.document.createElement("th")
      td.innerText = "author"
      td
    }
    table.appendChild {
      val td = dom.document.createElement("th")
      td.innerText = "title"
      td
    }
    corpus.foreach(b => renderBookRow(table, b))
    if (targetNode.hasChildNodes())
      targetNode.replaceChild(table, targetNode.firstChild)
    else targetNode.appendChild(table)
    ()
  }

  def setupUI(): Unit = {
    val button = document.createElement("button")
    button.textContent = "Render Table"
    val bookTable = dom.document.createElement("div")
    bookTable.id = "book-table"
    button.addEventListener(
      "click",
      (_: dom.MouseEvent) => renderBookTable(bookTable),
    )
    document.body.appendChild(button)
    document.body.appendChild(bookTable)
    ()
  }

  def main(args: Array[String]): Unit =
    document.addEventListener(
      "DOMContentLoaded",
      (_: dom.Event) => setupUI(),
    )
}
