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

class PositionalPostingsListSuite extends munit.FunSuite {

  test("PositionalPostingsList docs returns no docs for empty PostingsList") {
    val ppb = new PositionalPostingsBuilder
    val posList = ppb.toPositionalPostingsList
    assertEquals(posList.docs.toList, Nil)
  }

  test("PositionalPostingsList docs returns correct one doc") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    val posList = ppb.toPositionalPostingsList
    assertEquals(posList.docs.toList, List(1))
  }

  test("PositionalPostingsList docs returns correct two docs") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(42, 1)
    val posList = ppb.toPositionalPostingsList
    assertEquals(posList.docs.toList, List(1, 42))
  }

  test("PositionalPostingsList docs returns correct six docs") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(2, 1)
    ppb.addTermPosition(2, 2)
    ppb.addTermPosition(2, 3)
    ppb.addTermPosition(3, 33)
    ppb.addTermPosition(4, 44)
    ppb.addTermPosition(5, 55)
    ppb.addTermPosition(6, 66)
    val posList = ppb.toPositionalPostingsList
    assertEquals(posList.docs.toList, List(1, 2, 3, 4, 5, 6))
  }
}
