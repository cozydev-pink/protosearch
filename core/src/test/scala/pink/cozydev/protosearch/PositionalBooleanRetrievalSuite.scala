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
import pink.cozydev.lucille.QueryParser

class PositionalBooleanRetrievalSuite extends munit.FunSuite {

  val index = PositionalIndex(CatIndex.docs)
  val analyzer = Analyzer.default

  def search(qStr: String): Either[String, Set[Int]] =
    QueryParser
      .parse(qStr)
      .flatMap(q => PositionalBooleanRetrieval(index).search(q))

  test("Term") {
    val q = search("fast")
    assertEquals(
      q,
      Right(Set(1)),
    )
  }

  test("multi Term") {
    val q = search("fast cat")
    assertEquals(
      q,
      Right(Set(0, 1, 2)),
    )
  }

  test("And") {
    val q = search("fast AND cat")
    assertEquals(
      q,
      Right(Set(1)),
    )
  }

  test("Double And") {
    val q = search("the AND fast AND cat")
    assertEquals(
      q,
      Right(Set(1)),
    )
  }

  test("Or") {
    val q = search("fast OR cat")
    val results = Set(0, 1, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("Double Or") {
    val q = search("the OR fast OR cat")
    val results = Set(0, 1, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("cat AND (fast OR quick)") {
    val q = search("cat AND (fast OR quick)")
    val results = Set(0, 1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("cat AND NOT fast") {
    val q = search("cat AND NOT fast")
    val results = Set(0, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("[a TO z]") {
    val q = search("[a TO z]")
    val results = Set(0, 1, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("f*") {
    val q = search("f*")
    val results = Set(0, 1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("sleeps*") {
    val q = search("sleeps*")
    val results = Set(2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, single word \"very\"") {
    val q = search("\"very\"")
    val results = Set(1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, multi word, single match \"very fast\"") {
    val q = search("\"very fast\"")
    val results = Set(1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, multi word(5), single match") {
    val q = search("\"the very fast cat jumped\"")
    val results = Set(1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, multi word(7), repeated words, single match") {
    val q = search("\"the very fast cat jumped across the\"")
    val results = Set(1)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, multi word, multi match \"lazy cat\"") {
    val q = search("\"lazy cat\"")
    val results = Set(0, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("phrase, multi word, false match \"sleeps day\"") {
    val q = search("\"sleeps day\"")
    // assertNotEquals(q, Right(Set(2)))
    assertEquals(q, Right(Set.empty[Int]))
  }

}
