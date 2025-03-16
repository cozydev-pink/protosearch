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
import cats.effect.kernel.Resource
import laika.api.Transformer
import laika.ast.Path
import laika.config.SyntaxHighlighting
import laika.format.Markdown
import laika.io.api.BinaryTreeTransformer
import laika.io.model.InputTree
import laika.io.syntax.*
import munit.CatsEffectSuite
import scodec.bits.ByteVector

class IndexFormatSuite extends CatsEffectSuite {

  val transformer: Resource[IO, BinaryTreeTransformer[IO]] =
    Transformer
      .from(Markdown)
      .to(IndexFormat)
      .using(Markdown.GitHubFlavor, SyntaxHighlighting)
      .parallel[IO]
      .build

  def renderIndex(str: String): IO[MultiIndex] = {
    val tree = InputTree[IO].addString(str, Path.Root / "client" / "doc.md")
    val bytes = fs2.io
      .readOutputStream[IO](1024)(out =>
        transformer.use(_.fromInput(tree).toStream(IO(out)).transform)
      )
      .compile
      .toVector
    val index = bytes.map(bs => MultiIndex.codec.decodeValue(ByteVector(bs).toBitVector))
    index.map(i => i.require)
  }

  test("stores body field") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val body = renderIndex(doc).map(idx => idx.fields.get("body").map(_.toList))
    assertIO(body, Some(List("The Title\nnormal bold italics code\n")))
  }

  test("stores title field") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val title = renderIndex(doc).map(idx => idx.fields.get("title").map(_.toList))
    assertIO(title, Some(List("The Title")))
  }

  test("stores title field with extracted text") {
    val doc =
      """|# The Title `hasSpan`
         |normal **bold** *italics* `code`
         |""".stripMargin
    val title = renderIndex(doc).map(idx => idx.fields.get("title").map(_.toList))
    assertIO(title, Some(List("The Title hasSpan")))
  }

  test("stores path field without .txt suffix") {
    val doc =
      """|# The Title
         |normal **bold** *italics* `code`
         |""".stripMargin
    val path = renderIndex(doc).map(idx => idx.fields.get("path").map(_.toList))
    assertIO(path, Some(List("client/doc")))
  }

}
