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

/** A search request.
  *
  * @param query the query string to search for
  * @param size maximum number of results to return
  * @param highlightFields fields to highlight. `None` highlights all stored fields,
  *   `Some(Nil)` disables highlighting, `Some(list)` highlights only the named fields.
  * @param resultFields fields to return. `None` returns all stored fields,
  *   `Some(Nil)` returns no fields, `Some(list)` returns only the named fields.
  * @param lastTermPrefix if true, the last term in the query is treated as a prefix
  */
final case class SearchRequest(
    query: String,
    size: Int,
    highlightFields: Option[List[String]],
    resultFields: Option[List[String]],
    lastTermPrefix: Boolean,
)
object SearchRequest {
  private val defaultSize = 10

  /* Build a simple request with no highlighting, stored fields, or prefix rewriting */
  def default(query: String): SearchRequest =
    SearchRequest(query, defaultSize, Some(Nil), Some(Nil), false)
}
