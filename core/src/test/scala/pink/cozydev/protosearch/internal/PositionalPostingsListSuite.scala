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

  test("PositionalPostingsList cannot be empty") {
    val ppb = new PositionalPostingsBuilder
    intercept[java.lang.IllegalArgumentException] {
      ppb.toPositionalPostingsList
    }
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

  test("PositionalPostingsList Reader nextDoc(i < max) does not advance past i") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(2, 1)
    ppb.addTermPosition(2, 2)
    ppb.addTermPosition(2, 3)
    ppb.addTermPosition(3, 33)
    ppb.addTermPosition(42, 1)
    val posList = ppb.toPositionalPostingsList.reader()
    (0 to 10).foreach(_ => posList.nextDoc(2))
    assertEquals(posList.nextDoc(2), 2)
  }

  test("PositionalPostingsList Reader nextDoc(i > max) does not advance past max") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(2, 1)
    ppb.addTermPosition(2, 2)
    ppb.addTermPosition(2, 3)
    ppb.addTermPosition(3, 33)
    ppb.addTermPosition(42, 1)
    val posList = ppb.toPositionalPostingsList.reader()
    (0 to 10).foreach(_ => posList.nextDoc(100))
    assertEquals(posList.nextDoc(100), 42)
  }

  test("PositionalPostingsList Reader nextPosition(i < max) does not advance past i") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(2, 1)
    ppb.addTermPosition(2, 2)
    ppb.addTermPosition(2, 3)
    ppb.addTermPosition(3, 33)
    ppb.addTermPosition(42, 1)
    val posReader = ppb.toPositionalPostingsList.reader()
    posReader.nextDoc(2)
    (0 to 10).foreach(_ => posReader.nextPosition(2))
    assertEquals(posReader.nextPosition(2), 2)
  }

  test("PositionalPostingsList Reader nextPosition(i > max) does not advance past max") {
    val ppb = new PositionalPostingsBuilder
    ppb.addTermPosition(1, 3)
    ppb.addTermPosition(1, 8)
    ppb.addTermPosition(2, 1)
    ppb.addTermPosition(2, 2)
    ppb.addTermPosition(2, 3)
    ppb.addTermPosition(3, 33)
    ppb.addTermPosition(42, 1)
    val posReader = ppb.toPositionalPostingsList.reader()
    posReader.nextDoc(2)
    (0 to 10).foreach(_ => posReader.nextPosition(200))
    assertEquals(posReader.nextPosition(200), 3)
  }

  test("test 'guide is for' from http4s") {
    val ppl1 = new PositionalPostingsList(Array(1, 2, 100, 200, 3, 2, 0, 1))
    val ppl2 = new PositionalPostingsList(Array(0, 2, 10, 20, 1, 3, 15, 150, 250, 3, 1, 2))
    val ppl3 = new PositionalPostingsList(Array(0, 1, 7, 1, 2, 33, 333, 3, 1, 3))
    val pi = new PositionalIter(Array(ppl1.reader(), ppl2.reader(), ppl3.reader()), Array(1, 2, 3))
    val res = pi.takeWhile(_ > -1).toList
    assertEquals(res, List(3))
  }

}
