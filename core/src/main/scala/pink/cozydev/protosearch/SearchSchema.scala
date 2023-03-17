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

import pink.cozydev.protosearch.MultiIndex
import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.protosearch.analysis.QueryAnalyzer

/**  For a type `A`, a SearchSchema describes the fields of the document representation
  *  of `A`.
  */
case class SearchSchema[A] private (
    private val schema: Map[String, (A => String, Analyzer)]
) {
  def queryAnalyzer(defaultField: String): QueryAnalyzer = {
    val analyzers = schema.view.mapValues(_._2).toList
    QueryAnalyzer(defaultField, analyzers.head, analyzers.tail: _*)
  }

  def indexBldr(defaultField: String): List[A] => MultiIndex = {
    val ss = schema.toList
    MultiIndex(defaultField, ss.head, ss.tail: _*)
  }
}
object SearchSchema {
  def apply[A](
      head: (String, (A => String, Analyzer)),
      tail: (String, (A => String, Analyzer))*
  ): SearchSchema[A] =
    new SearchSchema[A]((head :: tail.toList).toMap)
}

object Example {
  case class Doc(title: String, author: String)

  val analyzer = Analyzer.default

  val s = SearchSchema[Doc](
    "title" -> (_.title, analyzer),
    "author" -> (_.author, analyzer),
  )
}
