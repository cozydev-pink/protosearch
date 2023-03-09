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

package pink.cozydev.protosearch.codecs

import scodec.{Attempt, Codec, codecs}

class IndexCodecsSuite extends munit.FunSuite {
  val termL = List(
    "hi",
    "hello",
    "goodbye",
  )

  test("IndexCodecs.termList encodes") {
    val terms = termL.toArray
    val bytes = IndexCodecs.termList.encode(terms)
    assert(bytes.isSuccessful)
  }

  test("IndexCodecs.termList round trips") {
    val terms = termL.toArray
    val bytes = IndexCodecs.termList.encode(terms)
    val termsDecoded = bytes.flatMap(IndexCodecs.termList.decodeValue)
    assert(termsDecoded.isSuccessful)
  }

  val intArray = Array(1, 2, 3, 30, 20, 10, 100, 200, 300)

  test("IndexCodecs.arrayOfN builds round tripping codecs") {
    val codec: Codec[Array[Int]] = IndexCodecs.arrayOfN(codecs.vint, codecs.vint)
    val bytes = codec.encode(intArray)
    val arrayDecoded = bytes.flatMap(codec.decodeValue)
    val decodedList = arrayDecoded.map(_.toList)
    val expected = Attempt.Successful(intArray.toList)
    assertEquals(decodedList, expected)
  }

  test("IndexCodecs.postings round trips") {
    val array2 = Array(
      Array(345, 678),
      intArray,
      Array(0, 9, 7),
    )
    val bytes = IndexCodecs.postings.encode(array2)
    val array2Decoded = bytes.flatMap(IndexCodecs.postings.decodeValue)
    val decodedList = array2Decoded.map(aa => aa.map(_.toList).toList)
    val expected = Attempt.Successful(array2.map(_.toList).toList)
    assertEquals(decodedList, expected)
  }

}
