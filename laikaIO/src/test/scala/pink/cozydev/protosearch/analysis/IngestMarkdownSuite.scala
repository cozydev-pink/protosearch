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

class IngestMarkdownSuite extends munit.FunSuite {

  test("laika renders single header doc into plaintext sub doc") {
    val doc =
      """|
         |# Title
         |
         |**bold** *italics* `code`
         |""".stripMargin

    val subDocs = IngestMarkdown.transform(doc)
    val d1 = "bold italics code"
    val headings = NonEmptyList.one(
      SubDocument("doc", Some("title"), "Title", d1)
    )
    assertEquals(subDocs, Right(headings))
  }

  test("laika renders complex doc into plaintext sub documents") {
    val doc =
      """|
         |# Title
         |
         |sub title line
         |
         |## Introduction
         |
         |intro text with **bold**
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

    val subDocs = IngestMarkdown.transform(doc)
    val d1 = """|sub title line
                |
                |""".stripMargin
    val d2 = """|intro text with bold
                |val x = 2 + 2
                |inline code
                |
                |  quote!
                |""".stripMargin
    val d3 = """|read more here""".stripMargin
    val headings = NonEmptyList.of(
      SubDocument("doc", Some("introduction"), "Introduction", d2),
      SubDocument("doc", Some("the-conclusion"), "The Conclusion", d3),
      SubDocument("doc", Some("title"), "Title", d1),
    )
    assertEquals(subDocs, Right(headings))
  }

  test("laika renders no header doc into plaintext sub doc") {
    val doc =
      """|
         |No header
         |
         |**bold** *italics* `code`
         |""".stripMargin

    val subDocs = IngestMarkdown.transform(doc)
    val d1 = """|No header
                |bold italics code""".stripMargin
    val headings = NonEmptyList.one(
      SubDocument("doc", None, "doc", d1)
    )
    assertEquals(subDocs, Right(headings))
  }

  test("laika renders late header doc into two plaintext sub docs") {
    val doc =
      """|
         |No header
         |
         |**bold** *italics* `code`
         |
         |# Late header
         |
         |body text
         |""".stripMargin

    val subDocs = IngestMarkdown.transform(doc)
    val d1 = """|No header
                |bold italics code""".stripMargin
    val d2 = "body text"
    val headings = NonEmptyList.of(
      SubDocument("doc", None, "doc", d1),
      SubDocument("doc", Some("late-header"), "Late header", d2),
    )
    assertEquals(subDocs, Right(headings))
  }

  test("laika renders content of callout") {
    val doc =
      """|
         |# Title
         |
         |@:callout(warning)
         |warning text
         |@:@
         |
         |**bold** *italics* `code`
         |""".stripMargin

    val subDocs = IngestMarkdown.transform(doc)
    val d1 = """|warning text
                |bold italics code""".stripMargin
    val headings = NonEmptyList.one(
      SubDocument("doc", Some("title"), "Title", d1)
    )
    assertEquals(subDocs, Right(headings))
  }

}
