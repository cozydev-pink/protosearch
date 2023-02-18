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

class IngestSuite extends munit.FunSuite {
  val doc = """|
               |# Title
               |
               |sub title line
               |
               |## H2 Section
               |
               |h2 section text
               |
               |## Second H2
               |
               |second section text
               |""".stripMargin

  test("laika extracts headings".fail) {
    val x = Ingest.transform(doc)
    val headings = List("H2 Section", "Second H2", "Title")
    assertEquals(x, Right(headings))
  }

}
