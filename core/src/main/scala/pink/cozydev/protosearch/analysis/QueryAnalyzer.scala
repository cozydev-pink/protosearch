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

import cats.data.NonEmptyList
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.QueryParser

import pink.cozydev.lucille.MultiQuery

// TODO This is a hack, the Lucille parser tokenizes on white space only currently
// We perhaps want Lucille to use a tokenizer from textmogrify
// In the meantime, we rewrite the Query with our `Analyzer`
case class QueryAnalyzer(
    defaultField: String,
    analyzers: Map[String, Analyzer],
) {
  private def analyzeTermQ(a: Analyzer, query: Query): Either[String, Query] =
    query match {
      case q: Query.Term =>
        val terms = NonEmptyList.fromFoldable(a.tokenize(q.str))
        // println(s"analyzeTerQ processing '$q' -> $terms")
        terms match {
          case None => Left(s"Error tokenizing Term during analyzeTermQ: $q")
          case Some(ts) =>
            ts match {
              case NonEmptyList(head, Nil) => Right(Query.Term(head))
              case terms => Right(Query.Group(terms.map(Query.Term.apply)))
            }
        }
      case q: Query.Prefix => Right(q)
      case q: Query.TermRange => Right(q)
      case q: Query.Phrase => Right(q)
      case q: Query.Or =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.Or(qs))
      case q: Query.And =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.And(qs))
      case q: Query.Not =>
        analyzeTermQ(a, q.q).map(qs => Query.Not(qs))
      case q: Query.Group =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.Group(qs))
      case q: Query.Field => Left(s"Oops, nested field query?: $q")
      case q: MultiQuery =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => MultiQuery(qs))
      case q: Query.UnaryMinus =>
        analyzeTermQ(a, q.q).map(qs => Query.UnaryMinus(qs))
      case q: Query.UnaryPlus =>
        analyzeTermQ(a, q.q).map(qs => Query.UnaryPlus(qs))
      case q: Query.Proximity => Right(q)
      case q: Query.Fuzzy => Right(q)
      case q: Query.TermRegex => Right(q)
      case q: Query.MinimumMatch => Right(q)
      case q: Query.Boost => Right(q)
    }

  private def analyzeQ(query: Query): Either[String, Query] =
    query match {
      case Query.Term(q) =>
        val qs: List[String] = analyzers(defaultField).tokenize(q)
        NonEmptyList.fromList(qs) match {
          case None => Left(s"Query analysis error, no terms found after tokenizing $query")
          case Some(qs) =>
            if (qs.length == 1) Right(Query.Term(qs.head))
            else
              Left(
                s"Query analysis error, Term tokenized into multiple terms, this should be supported, but isn't yet"
              )
        }
      case q: Query.Prefix => Right(q)
      case q: Query.TermRange => Right(q)
      case Query.Phrase(q) =>
        val qs: List[String] = analyzers(defaultField).tokenize(q)
        NonEmptyList.fromList(qs) match {
          case None => Left(s"Query analysis error, no terms found after tokenizing $query")
          // TODO This is also a hack, we shouldn't reconstruct a string!
          case Some(qs) => Right(Query.Phrase(qs.toList.mkString(" ")))
        }
      case q: Query.Or => q.qs.traverse(analyzeQ).map(Query.Or.apply)
      case q: Query.And => q.qs.traverse(analyzeQ).map(Query.And.apply)
      case q: Query.Not => analyzeQ(q.q).map(Query.Not.apply)
      case q: Query.Group => q.qs.traverse(analyzeQ).map(Query.Group.apply)
      case Query.Field(fn, q) =>
        analyzers.get(fn) match {
          case None => Left(s"Query analysis error, field $fn is not supported in query $query")
          case Some(a) => analyzeTermQ(a, q).map(qq => Query.Field(fn, qq))
        }
      case q: MultiQuery => q.qs.traverse(analyzeQ).map(MultiQuery.apply)
      case q: Query.UnaryMinus => analyzeQ(q.q).map(Query.UnaryMinus.apply)
      case q: Query.UnaryPlus => analyzeQ(q.q).map(Query.UnaryPlus.apply)
      case q: Query.Proximity => Right(q)
      case q: Query.Fuzzy => Right(q)
      case q: Query.TermRegex => Right(q)
      case q: Query.MinimumMatch => Right(q)
      case q: Query.Boost => Right(q)
    }

  def parse(queryString: String): Either[String, MultiQuery] = {
    val q: Either[String, MultiQuery] =
      QueryParser.parse(queryString)
    q.flatMap(mq => mq.qs.traverse(analyzeQ).map(qs => MultiQuery(qs)))
  }

}
object QueryAnalyzer {
  def apply(
      defaultField: String,
      head: (String, Analyzer),
      tail: (String, Analyzer)*
  ): QueryAnalyzer =
    QueryAnalyzer(defaultField, (head :: tail.toList).toMap)
}
