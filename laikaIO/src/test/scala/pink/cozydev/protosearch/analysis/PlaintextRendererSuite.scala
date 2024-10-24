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

import cats.effect.{IO, Resource}
import laika.api.{MarkupParser, Renderer, Transformer}
import laika.format.{Markdown, ReStructuredText}
import laika.config.SyntaxHighlighting
import laika.api.errors.TransformationError
import laika.ast.Path.Root
import laika.io.api.TreeTransformer
import laika.io.model.InputTree
import laika.io.syntax.*
import munit.CatsEffectSuite

class PlaintextRendererSuite extends CatsEffectSuite {

  val markdownParser: MarkupParser =
    MarkupParser.of(Markdown).using(Markdown.GitHubFlavor, SyntaxHighlighting).build
  val rstParser: MarkupParser =
    MarkupParser.of(ReStructuredText).build
  val plaintextRenderer: Renderer = Renderer.of(Plaintext).build

  val ioTransformer: Resource[IO, TreeTransformer[IO]] = Transformer
    .from(Markdown)
    .to(Plaintext)
    .using(Markdown.GitHubFlavor, SyntaxHighlighting)
    .parallel[IO]
    .build

  def transformMarkdown(input: String): Either[TransformationError, String] =
    markdownParser.parse(input).flatMap(d => plaintextRenderer.render(d))
  def transformRST(input: String): Either[TransformationError, String] =
    rstParser.parse(input).flatMap(d => plaintextRenderer.render(d))

  def transformWithTemplate(input: String, template: String): IO[String] = {
    val firstDoc =
      s"""|{% 
          |laika.template = custom.template.txt
          |%}
          |
          |$input""".stripMargin
    val secondDoc =
      """|Second Doc
         |==========
         |
         |Text
         |""".stripMargin
    val inputTree = InputTree[IO]
      .addString(firstDoc, Root / "doc-1.md")
      .addString(secondDoc, Root / "doc-2.md")
      .addString(template, Root / "custom.template.txt")
    ioTransformer.use {
      _.fromInput(inputTree).toMemory.transform.map(_.allDocuments.head.content)
    }
  }

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

  /** BlockContainers **************************************************** */

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

  test("titled block - reStructuredText") {
    val doc =
      """|.. caution::
         |
         | Line 1
         |
         | Line 2""".stripMargin
    val expected =
      """|Caution!
         |Line 1
         |Line 2
         |
         |""".stripMargin
    assertEquals(transformRST(doc), Right(expected))
  }

  test("figure with a caption and a legend - reStructuredText") {
    val doc =
      """|.. figure:: picture.jpg
         |
         | This is the *caption*
         |
         | And this is the legend""".stripMargin
    val expected =
      """|This is the caption
         |And this is the legend
         |
         |""".stripMargin
    assertEquals(transformRST(doc), Right(expected))
  }

  /** lists ************************************************************** */

  test("nested bullet lists") {
    val doc =
      """|* Bullet 1 - Line 1
         |
         |  Bullet 1 - Line 2
         |
         |  * Nested - Line 1
         |
         |    Nested - Line 2
         |
         |* Bullet 2 - Line 1
         |  Bullet 2 - Line 2
         |""".stripMargin
    val expected =
      """|Bullet 1 - Line 1
         |Bullet 1 - Line 2
         |
         |Nested - Line 1
         |Nested - Line 2
         |Bullet 2 - Line 1
         |Bullet 2 - Line 2
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
  }

  test("enum lists") {
    val doc =
      """|1. Item 1
         |2. Item *em* 2
         |3. Item 3
         |""".stripMargin
    val expected =
      """|Item 1
         |Item em 2
         |Item 3
         |
         |""".stripMargin
    assertEquals(transformMarkdown(doc), Right(expected))
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

  test("exclude navigation lists") {
    val doc =
      """|First Doc
         |=========
         |""".stripMargin
    val template =
      """|@:navigationTree {
         |  entries = [{ target = "/", excludeRoot = true }]
         |}
         |
         |${cursor.currentDocument.content}
         |""".stripMargin
    transformWithTemplate(doc, template).map { res =>
      assert(!res.contains("Second Doc"))
    }
  }

}
