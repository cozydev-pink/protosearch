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

import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.protosearch.analysis.QueryAnalyzer
import cats.data.NonEmptyList

case class Field(name: String, analyzer: Analyzer, stored: Boolean, indexed: Boolean)

/**  For a type `A`, a SearchSchema describes the fields of the document representation
  *  of `A`.
  */
class SearchSchema[A] private (
    private val fields: NonEmptyList[(Field, A => String)]
) {
  def queryAnalyzer(defaultField: String): QueryAnalyzer = {
    val analyzers = fields.map { case (f, _) => (f.name, f.analyzer) }
    QueryAnalyzer(defaultField, analyzers.head, analyzers.tail: _*)
  }

  def indexBldr(defaultField: String): List[A] => MultiIndex =
    MultiIndex(defaultField, fields.head, fields.tail: _*)

}
object SearchSchema {
  def apply[A](
      head: (Field, A => String),
      tail: (Field, A => String)*
  ): SearchSchema[A] =
    new SearchSchema[A](NonEmptyList(head, tail.toList))
}
