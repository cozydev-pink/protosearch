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

import pink.cozydev.protosearch.analysis.Analyzer
import fixtures.BookIndex

class IndexBuilderSuite extends munit.FunSuite {
  import BookIndex.*

  val analyzer = Analyzer.default.withLowerCasing

  test("IndexBuilder.of first field is default") {
    val bldr = IndexBuilder
      .of[Book](
        (Field("title", analyzer, true, true, true), _.title),
        (Field("author", analyzer, true, true, false), _.author)
      )
    assertEquals(bldr.defaultField, "title")
  }

  test("IndexBuilder#fromList builds index from list") {
    val bldr = IndexBuilder
      .of[Book](
        (Field("title", analyzer, true, true, true), _.title),
        (Field("author", analyzer, true, true, false), _.author)
      )
    val index = bldr.fromList(allBooks)
    assertEquals(index.numDocs, allBooks.size)
  }

}
