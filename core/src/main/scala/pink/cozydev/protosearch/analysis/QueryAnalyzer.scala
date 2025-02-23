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

package pink.cozydev.protosearch.analysis

import pink.cozydev.lucille.{Query, TermQuery}
import pink.cozydev.lucille.Query._
import pink.cozydev.lucille.QueryParser

// TODO This is a hack, the Lucille parser tokenizes on white space only currently
// We perhaps want Lucille to use a tokenizer from textmogrify
// In the meantime, we rewrite the Query with our `Analyzer`
final case class QueryAnalyzer(
    defaultField: String,
    analyzers: Map[String, Analyzer],
) {
  // TODO Support using the right analyzer for the right field
  private val defaultAnalyzer = analyzers(defaultField)

  private def analyzeTermQ(query: TermQuery): Either[String, Query] =
    query match {
      case Term(t) =>
        defaultAnalyzer.tokenize(t) match {
          case Nil => Left(s"Error tokenizing Term '$t' during query analysis")
          case q1 :: Nil => Right(Term(q1))
          case q1 :: q2 :: tail => Right(Or(Term(q1), Term(q2), tail.map(Term.apply)))
        }
      case Phrase(p) =>
        defaultAnalyzer.tokenize(p) match {
          case Nil => Left(s"Error tokenizing Phrase '$p' during query analysis")
          // TODO This is also a hack, we shouldn't reconstruct a string!
          case terms => Right(Phrase(terms.mkString(" ")))
        }
      case q => Right(q)
    }

  def parse(queryString: String): Either[String, Query] =
    QueryParser.parse(queryString).flatMap(q => q.traverseQ(analyzeTermQ))

}
object QueryAnalyzer {
  def apply(
      defaultField: String,
      head: (String, Analyzer),
      tail: (String, Analyzer)*
  ): QueryAnalyzer =
    QueryAnalyzer(defaultField, (head :: tail.toList).toMap)
}
