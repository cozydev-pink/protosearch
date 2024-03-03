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

class FrequencyIndexCodecSuite extends munit.FunSuite {

  val index = FrequencyIndex(fixtures.CatIndex.docs)

  test("FrequencyIndex.codec encodes") {
    val bytes = FrequencyIndex.codec.encode(index)
    assert(bytes.isSuccessful)
  }

  test("FrequencyIndex.codec round trips") {
    val bytes = FrequencyIndex.codec.encode(index)
    val indexDecoded = bytes.flatMap(FrequencyIndex.codec.decodeValue)
    assert(indexDecoded.isSuccessful)
  }

  test("PositionalIndex.codec errors when trying to decode a FrequencyIndex") {
    val bytes = FrequencyIndex.codec.encode(index)
    // oops, using the wrong decoder
    val indexDecoded = bytes.flatMap(PositionalIndex.codec.decodeValue)
    assert(indexDecoded.isFailure)
  }

}
