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

class NotQueryIteratorSuite extends munit.FunSuite {

  val allDocs = Set(1, 2, 3, 4)

  def checkNotMatchesInverse(
      queryIter: () => QueryIterator
  )(implicit
      loc: munit.Location
  ): Unit = {
    val origDocs = queryIter().docs.toSet
    val iter = new NotQueryIterator(queryIter(), alphaNumIndex.numDocs)
    val notDocs = iter.docs.toSet
    assertEquals(notDocs, allDocs -- origDocs, clue = s"origDocs=$origDocs")
  }

  def checkNot(
      queryIter: () => QueryIterator,
      expected: Set[Int],
  )(implicit
      loc: munit.Location
  ): Unit = {
    val iter = new NotQueryIterator(queryIter(), alphaNumIndex.numDocs)
    val notDocs = iter.docs.toSet
    assertEquals(notDocs, expected)
  }

  test("NOT(match doc 1) matches docs 2, 3, 4") {
    val qi = TestQuery.exact("doc1", "0 1 2 3")
    checkNotMatchesInverse(qi.iter)
    checkNot(qi.iter, Set(2, 3, 4))
  }

  test("NOT(match doc 2) matches docs 1, 3, 4") {
    val qi = TestQuery.exact("doc2", "a b c d")
    checkNotMatchesInverse(qi.iter)
    checkNot(qi.iter, Set(1, 3, 4))
  }

  test("NOT(match doc 4) matches docs 1, 2, 3") {
    val qi = TestQuery.exact("doc4", "0 b 2 d")
    checkNotMatchesInverse(qi.iter)
    checkNot(qi.iter, Set(1, 2, 3))
  }

  test("NOT(NOT(match doc 1)) matches doc 1") {
    val d1 = TestQuery.exact("doc1", "0 1 2 3")
    val qi = () => new NotQueryIterator(d1.iter(), alphaNumIndex.numDocs)
    checkNotMatchesInverse(qi)
    checkNot(qi, Set(1))
  }

  test("NOT(match none) matches all") {
    val none = TestQuery.noMatch
    checkNotMatchesInverse(none.iter)
    checkNot(none.iter, Set(1, 2, 3, 4))
  }

  test("NOT(NOT(match none)) matches none") {
    val d1 = TestQuery.noMatch
    val qi = () => new NotQueryIterator(d1.iter(), alphaNumIndex.numDocs)
    checkNotMatchesInverse(qi)
    checkNot(qi, Set())
  }

}
