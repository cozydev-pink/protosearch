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

final case class SearchRequest(
    query: String,
    size: Int,
    highlightFields: Option[List[String]],
    resultFields: Option[List[String]],
    lastTermPrefix: Boolean,
    // sort
    // query re-writing?
)
object SearchRequest {
  private val defaultSize = 10
  def default(query: String): SearchRequest =
    SearchRequest(query, defaultSize, Some(Nil), Some(Nil), false)
}
