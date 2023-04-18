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
import pink.cozydev.lucille.Parser

class BooleanRetrievalSuite extends munit.FunSuite {

  val index = CatIndex.index
  val analyzer = Analyzer.default

  test("Term") {
    val q = Parser.parseQ("fast").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(Set(1)),
    )
  }

  test("multi Term") {
    val q = Parser.parseQ("fast cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(Set(1)),
    )
  }

  test("And") {
    val q = Parser.parseQ("fast AND cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(Set(1)),
    )
  }

  test("Double And") {
    val q = Parser.parseQ("the AND fast AND cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(Set(1)),
    )
  }

  test("Or") {
    val q = Parser.parseQ("fast OR cat").map(_.head)
    val results = Set(0, 1, 2)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("Double Or") {
    val q = Parser.parseQ("the OR fast OR cat").map(_.head)
    val results = Set(0, 1, 2)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("cat AND (fast OR quick)") {
    val q = Parser.parseQ("cat AND (fast OR quick)").map(_.head)
    val results = Set(0, 1)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("cat AND NOT fast") {
    val q = Parser.parseQ("cat AND NOT fast").map(_.head)
    val results = Set(0, 2)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("[a TO z]") {
    val q = Parser.parseQ("[a TO z]").map(_.head)
    val results = Set(0, 1, 2)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("f*") {
    val q = Parser.parseQ("f*").map(_.head)
    val results = Set(0, 1)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

  test("sleeps*") {
    val q = Parser.parseQ("sleeps*").map(_.head)
    val results = Set(2)
    assertEquals(
      q.flatMap(q => BooleanRetrieval(index).search(q)),
      Right(results),
    )
  }

}
