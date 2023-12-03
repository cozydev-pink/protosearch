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
import laika.format.Markdown
import laika.config.SyntaxHighlighting
import laika.api.Renderer
import laika.api.errors.TransformationError

class PlaintextRendererSuite extends munit.FunSuite {

  val parser = MarkupParser.of(Markdown).using(Markdown.GitHubFlavor, SyntaxHighlighting).build
  val plaintextRenderer = Renderer.of(Plaintext).build

  def render(input: String): Either[TransformationError, String] =
    parser.parse(input).flatMap(d => plaintextRenderer.render(d))

  test("title, words") {
    val doc =
      """|
         |# Title
         |**bold** *italics* `code`
         |""".stripMargin
    val expected =
      """|Title
         |bold italics code""".stripMargin
    assertEquals(render(doc), Right(expected))
  }

}
