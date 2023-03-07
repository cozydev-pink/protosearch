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

import pink.cozydev.protosearch.analysis.TokenStream.tokenizeSpaceL

object CatIndex {
  val docs: List[List[String]] =
    List(
      tokenizeSpaceL("the quick brown fox jumped over the lazy cat"),
      tokenizeSpaceL("the very fast cat jumped across the room"),
      tokenizeSpaceL("a lazy cat sleeps all day"),
    )

  lazy val index = TermIndexArray(docs)
}
