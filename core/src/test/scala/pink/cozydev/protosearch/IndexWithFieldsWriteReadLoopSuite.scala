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
import scodec.Codec

class IndexWithFieldsWriteReadLoopSuite extends munit.FunSuite {
  import BookIndex.{Book, allBooks, fish}

  test("Can write bytes, read bytes, and search") {
    val analyzer = Analyzer.default.withLowerCasing

    val index = MultiIndex.apply[Book](
      "title",
      ("title", _.title, analyzer),
      ("author", _.author, analyzer),
    )(allBooks)

    val indexWithFields = IndexWithFields(index, allBooks)
    val bookCodec: Codec[Book] = (IndexWithFields.strCodec :: IndexWithFields.strCodec)
      .as[(String, String)]
      .xmap(
        { case (t: String, a: String) => Book(t, a) },
        { case (b: Book) => (b.title, b.author) },
      )

    val indexBytes = IndexWithFields.codec[Book](bookCodec).encode(indexWithFields).map(_.bytes)

    val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

    def search(indexWithFields: IndexWithFields[Book])(qs: String): Either[String, List[Book]] = {
      val q = qAnalyzer.parse(qs)
      val result = q.flatMap(mq => indexWithFields.search(mq.qs))
      result
    }

    val indexRead = indexBytes
      .flatMap(bv => IndexWithFields.codec[Book](bookCodec).decodeValue(bv.bits))
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
