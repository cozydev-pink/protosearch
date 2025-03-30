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

class OrQueryIteratorSuite extends munit.FunSuite {

  // The order of QueryIterators should not matter, so we test all permutations
  def checkAllPermutations(
      queries: List[TestQuery],
      expected: List[Int],
      minShouldMatch: Int,
  )(implicit
      loc: munit.Location
  ): Unit =
    queries.permutations.foreach { tqs =>
      val name = tqs.map(_.label).mkString(", ")
      val iter = OrQueryIterator(tqs.map(tq => tq.iter()).toArray, minShouldMatch)
      val docs = iter.docs.toList
      assertEquals(docs, expected, clue = name)
    }

  test("always match with 1 matching, minShouldMatch=1 (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1), minShouldMatch = 1)
  }

  test("always match with 1 matching, minShouldMatch=1 (doc2)") {
    val queries = List(
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(2), minShouldMatch = 1)
  }

  test("always match with 1 matching, minShouldMatch=1 (last doc)") {
    assert(alphaNumIndex.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(4), minShouldMatch = 1)
  }

  test("never match with 1 matching, minShouldMatch=2 (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("never match with 1 matching, minShouldMatch=2 (doc2)") {
    val queries = List(
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("never match with 1 matching, minShouldMatch=2 (last doc)") {
    assert(alphaNumIndex.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("always match with 2 matching, minShouldMatch=1") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "4 5 6 7"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.exact("doc2", "e f g h"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1, 2), minShouldMatch = 1)
  }

  test("always match with 2 matching, minShouldMatch=2") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "4 5 6 7"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.exact("doc2", "e f g h"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1, 2), minShouldMatch = 2)
  }

  test("only match with 2 matching, minShouldMatch=2") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "4 5 6 7"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1), minShouldMatch = 2)
  }

  test("never match with 2 matching, minShouldMatch=3") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc1", "4 5 6 7"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 3)
  }
}
