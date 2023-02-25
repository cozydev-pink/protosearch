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

import pink.cozydev.lucille.Parser

class QuerySuite extends munit.FunSuite {

  val index = CatIndex.index
  val analyzer = Analyzer.default

  test("TermQ") {
    val q = Parser.parseQ("fast").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanQuery(index).search(q)),
      Right(List((1, 0.6931471805599453))),
    )
  }

  test("multi TermQ") {
    val q = Parser.parseQ("fast cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanQuery(index).search(q)),
      Right(List((1, 0.6931471805599453))),
    )
  }

  test("AndQ") {
    val q = Parser.parseQ("fast AND cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanQuery(index).search(q)),
      Right(List((1, 0.9241962407465937))),
    )
  }

  test("Double AndQ") {
    val q = Parser.parseQ("the AND fast AND cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanQuery(index).search(q)),
      Right(List((1, 1.4735023850806486))),
    )
  }

  test("OrQ") {
    val q = Parser.parseQ("fast OR cat").map(_.head)
    val results = List(
      (0, 0.23104906018664842),
      (1, 0.9241962407465937),
      (2, 0.23104906018664842),
    )
    assertEquals(q.flatMap(q => BooleanQuery(index).search(q)), Right(results))
  }

  test("Double OrQ") {
    val q = Parser.parseQ("the OR fast OR cat").map(_.head)
    val results = List(
      (0, 0.7803552045207033),
      (1, 1.4735023850806486),
      (2, 0.23104906018664842),
    )
    assertEquals(q.flatMap(q => BooleanQuery(index).search(q)), Right(results))
  }

  test("cat AND (fast OR quick)") {
    val q = Parser.parseQ("cat AND (fast OR quick)").map(_.head)
    val results = List(
      (0, 0.9241962407465937),
      (1, 0.9241962407465937),
    )
    assertEquals(q.flatMap(q => BooleanQuery(index).search(q)), Right(results))
  }

  test("cat AND NOT fast") {
    val q = Parser.parseQ("cat AND NOT fast").map(_.head)
    val results = List(
      (0, 0.23104906018664842),
      (2, 0.23104906018664842),
    )
    assertEquals(q.flatMap(q => BooleanQuery(index).search(q)), Right(results))
  }

}
