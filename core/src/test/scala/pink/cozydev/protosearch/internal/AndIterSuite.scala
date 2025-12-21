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

import TestQueryFixtures._

class AndIterSuite extends munit.FunSuite {

  // The order of QueryIterators should not matter, so we test all permutations
  def checkAllPermutations(
      queries: List[TestQuery],
      expected: List[Int],
  )(implicit
      loc: munit.Location
  ): Unit =
    queries.permutations.foreach { tqs =>
      val name = tqs.map(_.label).mkString(", ")
      val iter = new AndIter(tqs.map(tq => tq.iter()).toArray)
      val docs = iter.docs.toList
      assertEquals(docs, expected, clue = name)
    }

  test("never match with 1 noMatch (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty)
  }

  test("never match with 1 noMatch (doc2)") {
    val queries = List(
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty)
  }

  test("never match with 1 noMatch (last doc)") {
    assert(alphaNumIndex.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d"),
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty)
  }

  test("always match with all matching, 1 query (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3")
    )
    checkAllPermutations(queries, List(1))
  }

  test("always match with all matching, 1 query (last doc)") {
    assert(alphaNumIndex.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d")
    )
    checkAllPermutations(queries, List(4))
  }

  test("always match with all matching, multiple queries (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "3 4 5 6"),
      TestQuery.exact("doc1", "6 7 8 9"),
    )
    checkAllPermutations(queries, List(1))
  }

  test("never match when queries only match different docs") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "3 4 5 6"),
      TestQuery.exact("doc2", "a b c d"),
    )
    checkAllPermutations(queries, List.empty)
  }

  test("always match with all matching, multiple queries, multiple docs") {
    val queries = List(
      TestQuery.exact("doc1&3", "1"),
      TestQuery.exact("doc1&3", "3"),
    )
    checkAllPermutations(queries, List(1, 3))
  }
}
