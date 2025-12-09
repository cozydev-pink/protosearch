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
import internal.QueryIteratorSearch

class FrequencyIndexSearcherSuite extends munit.FunSuite {

  val index = fixtures.CatIndex.index
  val analyzer = Analyzer.default

  def search(qStr: String): Either[String, Set[Int]] =
    QueryParser
      .parse(qStr)
      .flatMap(q => QueryIteratorSearch(index, ScoreFunction.noScore).search(q))
      .map(_.toSet)

  test("Term") {
    val q = search("fast")
    assertEquals(
      q,
      Right(Set(2)),
    )
  }

  test("multi Term") {
    val q = search("fast cat")
    assertEquals(
      q,
      Right(Set(1, 2, 3)),
    )
  }

  test("And") {
    val q = search("fast AND cat")
    assertEquals(
      q,
      Right(Set(2)),
    )
  }

  test("Double And") {
    val q = search("the AND fast AND cat")
    assertEquals(
      q,
      Right(Set(2)),
    )
  }

  test("Or") {
    val q = search("fast OR cat")
    val results = Set(1, 2, 3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("Double Or") {
    val q = search("the OR fast OR cat")
    val results = Set(1, 2, 3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("cat AND (fast OR quick)") {
    val q = search("cat AND (fast OR quick)")
    val results = Set(1, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("cat AND NOT fast") {
    val q = search("cat AND NOT fast")
    val results = Set(1, 3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("[a TO z]") {
    val q = search("[a TO z]")
    val results = Set(1, 2, 3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("f*") {
    val q = search("f*")
    val results = Set(1, 2)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("sleeps*") {
    val q = search("sleeps*")
    val results = Set(3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("regex") {
    val q = search("/jump.*/ /cat/")
    val results = Set(1, 2, 3)
    assertEquals(
      q,
      Right(results),
    )
  }

  test("regex fail") {
    val q = search("/[/")
    val err = "Invalid regex query TermRegex([)"
    assertEquals(
      q,
      Left(err),
    )
  }

}
