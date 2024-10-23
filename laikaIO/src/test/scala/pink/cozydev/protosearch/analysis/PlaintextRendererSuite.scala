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

import laika.api.MarkupParser
import laika.format.{Markdown, ReStructuredText}
import laika.config.SyntaxHighlighting
import laika.api.Renderer
import laika.api.errors.TransformationError

class PlaintextRendererSuite extends munit.FunSuite {

  val markdownParser: MarkupParser =
    MarkupParser.of(Markdown).using(Markdown.GitHubFlavor, SyntaxHighlighting).build
  val rstParser: MarkupParser =
    MarkupParser.of(ReStructuredText).build
  val plaintextRenderer: Renderer = Renderer.of(Plaintext).build

  def transformMarkdown(input: String): Either[TransformationError, String] =
    markdownParser.parse(input).flatMap(d => plaintextRenderer.render(d))
  def transformRST(input: String): Either[TransformationError, String] =
    rstParser.parse(input).flatMap(d => plaintextRenderer.render(d))

  test("title, words") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val expected =
      """|The Title
         |normal bold italics code
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
  }

  test("title, empty line, words") {
    val doc =
      """|# The Title
         |
         |normal **bold** *italics* `code`
         |""".stripMargin
    val expected =
      """|The Title
         |normal bold italics code
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
  }

  test("title, empty line, words, code block") {
    val doc =
      """|# The Title
         |
         |normal
         |
         |```scala
         |val x = 2
         |```
         |""".stripMargin
    val expected =
      """|The Title
         |normal
         |val x = 2
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
  }

  test("nested blockquotes - Markdown") {
    val doc =
      """|>aaa
         |>
         |>>bbb
         |>
         |>ccc""".stripMargin
    val expected =
      """|aaa
         |bbb
         |
         |ccc
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
  }

  test("block quote with an attribution - reStructuredText") {
    val doc =
      """| Paragraph 1
         |
         | -- an attribution""".stripMargin
    val expected =
      """|Paragraph 1
         |an attribution
         |
         |""".stripMargin
    assertEquals(transformRST(doc), Right(expected))
  }

  test("definition list - reStructuredText") {
    val doc =
      """|term 1
         |   aaa
         |   aaa
         |
         |  bbb *ccc* ddd
         |
         |term 2
         |  ccc""".stripMargin
    val expected =
      """|term 1
         |aaa
         |aaa
         |
         |bbb ccc ddd
         |term 2
         |ccc
         |
         |""".stripMargin
    assertEquals(transformRST(doc), Right(expected))
  }

}
