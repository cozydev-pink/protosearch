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

import pink.cozydev.protosearch.MultiIndex

import cats.effect.IO
import laika.api.Transformer
import laika.api.config.ConfigBuilder
import laika.ast.Path
import laika.config.SyntaxHighlighting
import laika.format.Markdown
import laika.io.model.{InputTree, InputTreeBuilder}
import laika.io.syntax.*
import munit.CatsEffectSuite
import scodec.bits.ByteVector

class IndexFormatSuite extends CatsEffectSuite {

  val dir: Path = Path.Root / "client"

  def renderIndex(tree: InputTreeBuilder[IO], configKeys: List[String] = Nil): IO[MultiIndex] = {
    val transformer = Transformer
      .from(Markdown)
      .to(IndexFormat.withConfigKeys(configKeys))
      .using(Markdown.GitHubFlavor, SyntaxHighlighting)
      .parallel[IO]
      .build
    fs2.io
      .readOutputStream[IO](1024)(out =>
        transformer.use(_.fromInput(tree).toStream(IO(out)).transform)
      )
      .compile
      .toVector
      .map(bs => MultiIndex.codec.decodeValue(ByteVector(bs).toBitVector))
      .map(i => i.require)
  }

  test("stores body field") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val body = renderIndex(tree).map(idx => idx.fields.get("body").map(_.toList))
    assertIO(body, Some(List("The Title\nnormal bold italics code\n")))
  }

  test("stores title field") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val title = renderIndex(tree).map(idx => idx.fields.get("title").map(_.toList))
    assertIO(title, Some(List("The Title")))
  }

  test("stores title field with extracted text") {
    val doc =
      """|# The Title `hasSpan`
         |normal **bold** *italics* `code`
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val title = renderIndex(tree).map(idx => idx.fields.get("title").map(_.toList))
    assertIO(title, Some(List("The Title hasSpan")))
  }

  test("stores path field without .txt suffix") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val path =
      renderIndex(tree).map(idx => idx.fields.get("path").map(_.toList))
    assertIO(path, Some(List("/client/doc")))
  }

  test("config keys: indexes string value") {
    val doc =
      """|{%
         |  author: "Jane Doe"
         |%}
         |# The Title
         |Some content.
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val index = renderIndex(tree, configKeys = List("author"))
    val author = index.map(_.fields.get("author").map(_.toList))
    assertIO(author, Some(List("Jane Doe")))
  }

  test("config keys: indexes array values joined with spaces") {
    val doc =
      """|{%
         |  tags: [summits, events]
         |%}
         |# The Title
         |Some content.
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val index = renderIndex(tree, configKeys = List("tags"))
    val tags = index.map(_.fields.get("tags").map(_.toList))
    assertIO(tags, Some(List("summits events")))
  }

  test("config keys: missing key returns empty string") {
    val doc =
      """|# The Title
         |Some content.
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val index = renderIndex(tree, configKeys = List("author"))
    val author = index.map(_.fields.get("author").map(_.toList))
    assertIO(author, Some(List("")))
  }

  test("config keys: multiple documents with and without key") {
    val doc1 =
      """|{%
         |  author: "Jane Doe"
         |%}
         |# First
         |Content one.
         |""".stripMargin
    val doc2 =
      """|# Second
         |Content two.
         |""".stripMargin
    val tree = InputTree[IO]
      .addString(doc1, dir / "doc1.md")
      .addString(doc2, dir / "doc2.md")
    val index = renderIndex(tree, configKeys = List("author"))
    val author = index.map(_.fields.get("author").map(_.toList))
    assertIO(author, Some(List("Jane Doe", "")))
  }

  test("config keys: substitution variables resolved from directory config") {
    val doc =
      """|{%
         |  author: [${armanbilge}, ${valencik}]
         |%}
         |# The Title
         |Some content.
         |""".stripMargin
    val dirConfig = ConfigBuilder.empty
      .withValue("armanbilge", "Arman Bilge")
      .withValue("valencik", "Andrew Valencik")
      .build
    val tree = InputTree[IO]
      .addString(doc, dir / "doc.md")
      .addConfig(dirConfig, dir)
    val index = renderIndex(tree, configKeys = List("author"))
    val author = index.map(_.fields.get("author").map(_.toList))
    assertIO(author, Some(List("Arman Bilge Andrew Valencik")))
  }

  test("config keys: empty list adds no extra fields") {
    val doc =
      """|{%
         |  author: "Jane Doe"
         |%}
         |# The Title
         |Some content.
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val idx = renderIndex(tree, configKeys = Nil)
    val hasAuthor = idx.map(_.fields.contains("author"))
    assertIO(hasAuthor, false)
  }

  test("config keys: boolean and numeric values indexed as strings") {
    val doc =
      """|{%
         |  featured: true
         |  year: 2024
         |%}
         |# The Title
         |Some content.
         |""".stripMargin
    val tree = InputTree[IO].addString(doc, dir / "doc.md")
    val index = renderIndex(tree, configKeys = List("featured", "year"))
    val featured = index.map(_.fields.get("featured").map(_.toList))
    val year = index.map(_.fields.get("year").map(_.toList))
    assertIO(featured, Some(List("true")))
    assertIO(year, Some(List("2024")))
  }

}
