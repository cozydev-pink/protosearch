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

  val index = CatIndex.index

  test("apply builds from list of lists of strings") {
    assertEquals(index.numTerms, 16)
  }

  test("docCount returns zero when no docs contain term") {
    assertEquals(index.docCount("???"), 0)
  }

  test("docCount returns number of documents containing term") {
    assertEquals(index.docCount("cat"), 3)
    assertEquals(index.docCount("lazy"), 2)
    assertEquals(index.docCount("room"), 1)
  }

  test("docsWithTerm returns empty list when no docs contain term") {
    assertEquals(index.docsWithTerm("???"), Nil)
  }

  test("docsWithTerm returns list of docIDs containing term") {
    assertEquals(index.docsWithTerm("cat").sorted, List(0, 1, 2))
    assertEquals(index.docsWithTerm("the").sorted, List(0, 1))
    assertEquals(index.docsWithTerm("lazy").sorted, List(0, 2))
  }

  test("docsForPrefix returns empty list when no docs contain prefix") {
    assertEquals(index.docsForPrefix("x"), Set.empty)
  }

  test("termsForPrefix returns all terms starting with prefix") {
    assertEquals(index.termsForPrefix("f"), List("fast", "fox"))
  }

  test("termsForPrefix returns term if it exactly matches prefix") {
    assertEquals(index.termsForPrefix("sleeps"), List("sleeps"))
  }

  test("termsForPrefix returns empty list if no prefix matches") {
    assertEquals(index.termsForPrefix("x"), Nil)
  }

  test("Index.codec encodes") {
    val bytes = Index.codec.encode(index)
    assert(bytes.isSuccessful)
  }

  test("Index.codec round trips") {
    val bytes = Index.codec.encode(index)
    val indexDecoded = bytes.flatMap(Index.codec.decodeValue)
    assert(indexDecoded.isSuccessful)
  }

}
