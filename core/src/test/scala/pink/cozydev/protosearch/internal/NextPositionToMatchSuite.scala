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

class NextPositionToMatchSuite extends munit.FunSuite {
  import NextPositionToMatch._

  test("nextNotInOrderWithLargest chooses unordered position closest to being in order") {
    val arr = Array(1, 200, 199)
    assertEquals(nextNotInOrderWithLargest(arr), 2)
  }

  test("nextNotInOrderWithLargest single element returns -1") {
    val arr = Array(1)
    assertEquals(nextNotInOrderWithLargest(arr), -1)
  }

  test("nextNotInOrderWithLargest two in order returns -1") {
    val arr = Array(1, 2)
    assertEquals(nextNotInOrderWithLargest(arr), -1)
  }

  test("nextNotInOrderWithLargest in order returns -1") {
    val arr = Array(1, 2, 3, 4, 5, 6, 7, 8)
    assertEquals(nextNotInOrderWithLargest(arr), -1)
  }

  test("nextNotInOrderWithLargest 2, (1)") {
    val arr = Array(2, 1)
    assertEquals(nextNotInOrderWithLargest(arr), 1)
  }

  test("nextNotInOrderWithLargest 1, 2, (3), 40, 41, 42") {
    val arr = Array(1, 2, 3, 40, 41, 42)
    assertEquals(nextNotInOrderWithLargest(arr), 2)
  }

  test("nextNotInOrderWithLargest 40, 41, 42, (1), 2") {
    val arr = Array(40, 41, 42, 1, 2)
    assertEquals(nextNotInOrderWithLargest(arr), 3)
  }

}
