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
import pink.cozydev.lucille.Query.{TermQ, FieldQ, PrefixTerm, Group, OrQ}
import pink.cozydev.protosearch.LastTermRewrite._

class LastTermRewriteSuite extends munit.FunSuite {

  test("termToPrefix rewrites termQ to termQ and prefix") {
    val q = TermQ("f")
    val expected =
      Group(NonEmptyList.one(OrQ(NonEmptyList.of(TermQ("f"), PrefixTerm("f")))))
    assertEquals(termToPrefix(q), expected)
  }

  test("termToPrefix rewrites fieldQ to fieldQ's with termQ and prefix") {
    val q = FieldQ("fn", TermQ("c"))
    val expected =
      Group(
        NonEmptyList.one(
          OrQ(NonEmptyList.of(FieldQ("fn", TermQ("c")), FieldQ("fn", PrefixTerm("c"))))
        )
      )
    assertEquals(termToPrefix(q), expected)
  }

  test("lastTermPrefix rewrites only the last termQ to termQ and prefix") {
    val q =
      NonEmptyList.of(TermQ("first"), TermQ("f"))
    val expected =
      NonEmptyList.of(
        TermQ("first"),
        Group(NonEmptyList.one(OrQ(NonEmptyList.of(TermQ("f"), PrefixTerm("f"))))),
      )
    assertEquals(lastTermPrefix(q), expected)
  }
}
