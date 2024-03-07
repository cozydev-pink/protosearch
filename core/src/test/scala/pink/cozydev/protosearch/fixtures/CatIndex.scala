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

package pink.cozydev.protosearch.fixtures

import pink.cozydev.protosearch.FrequencyIndex
import pink.cozydev.protosearch.analysis.Analyzer

object CatIndex {
  val analyzer = Analyzer.default

  val docs: List[List[String]] =
    List(
      analyzer.tokenize("the quick brown fox jumped over the lazy cat"),
      analyzer.tokenize("the very fast cat jumped across the room"),
      analyzer.tokenize("a lazy cat sleeps all day"),
    )

  lazy val index = FrequencyIndex(docs)
}
