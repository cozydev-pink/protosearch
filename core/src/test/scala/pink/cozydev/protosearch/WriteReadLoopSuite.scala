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

import pink.cozydev.protosearch.analysis.{Analyzer, QueryAnalyzer}
import fixtures.BookIndex

class WriteReadLoopSuite extends munit.FunSuite {
  import BookIndex.{Book, allBooks, fish}

  test("Can write bytes, read bytes, and search") {
    val analyzer = Analyzer.default.withLowerCasing

    val index = IndexBuilder
      .of[Book](
        (Field("title", analyzer, true, true, true), _.title),
        (Field("author", analyzer, true, true, false), _.author),
      )
      .fromList(allBooks)

    val indexBytes = MultiIndex.codec.encode(index).map(_.bytes)

    val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

    def search(index: MultiIndex)(qs: String): Either[String, List[Book]] = {
      val q = qAnalyzer.parse(qs)
      val result = q.flatMap(mq => index.search(mq.qs))
      // TODO vector index access is unsafe
      result.map(hits => hits.map(i => allBooks(i)))
    }

    val indexRead = indexBytes
      .flatMap(bv => MultiIndex.codec.decodeValue(bv.bits))
      .toEither
      .left
      .map(_.toString)

    val results = indexRead.map(index =>
      search(index)("Two AND author:suess") match {
        case Left(_) => List.empty[Book]
        case Right(hits) => hits
      }
    )

    assertEquals(results, Right(List(fish)))
  }
}
