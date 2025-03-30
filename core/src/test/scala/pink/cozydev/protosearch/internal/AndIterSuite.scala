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

package pink.cozydev.protosearch.internal

import pink.cozydev.lucille.Query
import pink.cozydev.protosearch.fixtures.CatIndex
import pink.cozydev.protosearch.PositionalIndex

class AndIterSuite extends munit.FunSuite {

  val index = PositionalIndex(CatIndex.docs)

  test("do it") {
    val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
    val iter2 = PositionalIter.exact(index, Query.Phrase("the room")).get
    val iter = new AndIter(Array(iter1, iter2))
    val docs = iter.takeWhile(_ > -1).toSet
    assertEquals(docs, Set(2))
  }

  test("do it") {
    val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
    val iter2 = PositionalIter.exact(index, Query.Phrase("jumped")).get
    val iter3 = PositionalIter.exact(index, Query.Phrase("cat")).get
    val iter4 = PositionalIter.exact(index, Query.Phrase("the room")).get
    val iter = new AndIter(Array(iter1, iter2, iter3, iter4))
    val docs = iter.takeWhile(_ > -1).toSet
    assertEquals(docs, Set(2))
  }

  test("do it") {
    val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
    val iter2 = PositionalIter.exact(index, Query.Phrase("day")).get
    val iter = new AndIter(Array(iter1, iter2))
    val docs = iter.takeWhile(_ > -1).toSet
    assert(docs.isEmpty)
  }
}
