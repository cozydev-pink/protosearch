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

import cats.data.NonEmptyList
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.Query.{Term, Field, Prefix, Group, Or}
import pink.cozydev.protosearch.LastTermRewrite._
import pink.cozydev.lucille.MultiQuery

class LastTermRewriteSuite extends munit.FunSuite {

  test("termToPrefix rewrites termQ to termQ and prefix") {
    val q = Term("f")
    val expected =
      Group(NonEmptyList.one(Or(NonEmptyList.of(Term("f"), Prefix("f")))))
    assertEquals(termToPrefix(q), expected)
  }

  test("termToPrefix rewrites fieldQ to fieldQ's with termQ and prefix") {
    val q: Query = Field("fn", Term("c"))
    val expected =
      Field("fn", Group(NonEmptyList.one(Or(NonEmptyList.of(Term("c"), Prefix("c"))))))
    assertEquals(q.mapLastTerm(termToPrefix), expected)
  }

  test("termToPrefix rewrites only the last termQ to termQ and prefix") {
    val q =
      MultiQuery(Term("first"), Term("f"))
    val expected =
      MultiQuery(
        Term("first"),
        Group(NonEmptyList.one(Or(NonEmptyList.of(Term("f"), Prefix("f"))))),
      )
    assertEquals(q.mapLastTerm(termToPrefix), expected)
  }
}
