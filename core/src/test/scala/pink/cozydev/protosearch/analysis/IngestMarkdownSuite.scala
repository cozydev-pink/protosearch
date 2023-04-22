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

class IngestMarkdownSuite extends munit.FunSuite {
  val doc = """|
               |# Title
               |
               |sub title line
               |
               |## Introduction
               |
               |intro text with *bold*
               |
               |```scala
               |val x = 2 + 2
               |```
               |
               |inline `code`
               |
               |> quote!
               |
               |## The Conclusion
               |
               |read more [here](https://github.com/cozydev-pink/protosearch/)
               |""".stripMargin

  test("laika extracts headings") {
    val x = IngestMarkdown.transform(doc)
    val t = """|sub title line
               |
               |""".stripMargin
    val i = """|intro text with bold
               |val x = 2 + 2
               |inline code
               |
               |  quote!
               |""".stripMargin
    val c = """|read more here""".stripMargin
    val headings = List(
      SubDocument(Some("introduction"), "Introduction", i),
      SubDocument(Some("the-conclusion"), "The Conclusion", c),
      SubDocument(Some("title"), "Title", t),
    )
    assertEquals(x, Right(headings))
  }

}
