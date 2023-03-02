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

class IndexCodecsSuite extends munit.FunSuite {
  val index = CatIndex.index

  test("TermIndexCodec.termIndex encodes") {
    val bytes = TermIndexArray.codec.encode(index)
    assert(bytes.isSuccessful)
  }

  test("TermIndexCodec.termIndex round trips") {
    val bytes = TermIndexArray.codec.encode(index)
    val indexDecoded = bytes.flatMap(TermIndexArray.codec.decodeValue)
    assert(indexDecoded.isSuccessful)
  }

}
