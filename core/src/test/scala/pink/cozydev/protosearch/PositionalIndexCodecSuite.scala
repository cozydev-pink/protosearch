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

class PositionalIndexCodecSuite extends munit.FunSuite {

  val index = PositionalIndex(fixtures.CatIndex.docs)

  test("PositionalIndex.codec encodes") {
    val bytes = PositionalIndex.codec.encode(index)
    assert(bytes.isSuccessful)
  }

  test("PositionalIndex.codec round trips") {
    val bytes = PositionalIndex.codec.encode(index)
    val indexDecoded = bytes.flatMap(PositionalIndex.codec.decodeValue)
    assert(indexDecoded.isSuccessful)
  }

  // TODO Fix this, it can only lead to sorrow....
  test("FrequencyIndex.codec errors when trying to decode a PositionalIndex".fail) {
    val bytes = PositionalIndex.codec.encode(index)
    // oops, using the wrong decoder
    val indexDecoded = bytes.flatMap(FrequencyIndex.codec.decodeValue)
    assert(indexDecoded.isFailure)
  }

}
