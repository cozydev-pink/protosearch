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

sealed trait SearchResult {
  def fold[A](fail: String => A, success: List[Hit] => A): A = this match {
    case SearchFailure(msg) => fail(msg)
    case SearchSuccess(hits) => success(hits)
  }
}
final case class SearchFailure(msg: String) extends SearchResult
final case class SearchSuccess(hits: List[Hit]) extends SearchResult
