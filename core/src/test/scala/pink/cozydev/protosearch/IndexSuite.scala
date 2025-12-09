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

class IndexSuite extends munit.FunSuite {

  val index = fixtures.CatIndex.index

  test("apply builds from list of lists of strings") {
    assertEquals(index.numTerms, 16)
  }

  test("termIndexWhere returns zero when nonexistent would insert at beginning") {
    val indexB = FrequencyIndex(List(List("bb", "cc", "dd")))
    assertEquals(indexB.termDict.termIndexWhere("a"), 0)
  }

  test("termIndexWhere returns length of termDict when nonexistent would insert at end") {
    val indexB = FrequencyIndex(List(List("bb", "cc", "dd")))
    assertEquals(indexB.termDict.termIndexWhere("x"), 3)
  }

  test("termsForPrefix returns all terms starting with prefix") {
    assertEquals(index.termDict.termsForPrefix("f"), List("fast", "fox"))
  }

  test("termsForPrefix returns term if it exactly matches prefix") {
    assertEquals(index.termDict.termsForPrefix("sleeps"), List("sleeps"))
  }

  test("termsForPrefix returns empty list if no prefix matches") {
    assertEquals(index.termDict.termsForPrefix("x"), Nil)
  }

  test("termsForPrefix returns Nil if no prefix matches and it would insert at beginning") {
    val indexB = FrequencyIndex(List(List("bb")))
    assertEquals(indexB.termDict.termsForPrefix("a"), Nil)
  }

  test("Index.codec encodes") {
    val bytes = FrequencyIndex.codec.encode(index)
    assert(bytes.isSuccessful)
  }

  test("Index.codec round trips") {
    val bytes = FrequencyIndex.codec.encode(index)
    val indexDecoded = bytes.flatMap(FrequencyIndex.codec.decodeValue)
    assert(indexDecoded.isSuccessful)
  }

}
