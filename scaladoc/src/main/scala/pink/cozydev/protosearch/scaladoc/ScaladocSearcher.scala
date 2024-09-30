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

package pink.cozydev.protosearch.scaladoc

object ScaladocSearcher {
  def searchScaladoc(source: String, queries: List[String]): List[ScaladocInfo] = {
    val scaladocInfoList = ParseScaladoc.parseAndExtractInfo(source)
    val index = ScaladocIndexer.createScaladocIndex(scaladocInfoList)
    def search(q: String): List[ScaladocInfo] = {
      val searchResults = index.search(q)
      searchResults.fold(_ => Nil, hits => hits.map(h => scaladocInfoList.toList(h.id)))
    }

    val results: List[ScaladocInfo] = queries.flatMap { query =>
      search(query)
    }

    results
  }
}
