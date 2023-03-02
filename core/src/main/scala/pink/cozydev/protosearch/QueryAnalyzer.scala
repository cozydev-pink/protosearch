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

import cats.data.NonEmptyList
import cats.syntax.all._
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.Parser

// TODO ok this is ready to try
// TEST FAILING because we're not using it
// We perhaps want a new "Search" class or something that connects the Index with the QueryAnalyzer
// A "Search" should use a QueryAnalyzer, and then a QueryExecutor on an Index to return results
case class QueryAnalyzer(
    defaultField: String,
    analyzers: Map[String, Analyzer],
) {
  private def analyzeTermQ(a: Analyzer, query: Query): Either[String, Query] =
    query match {
      case q: Query.TermQ =>
        val terms = NonEmptyList.fromFoldable(a.tokenize(q.q))
        // println(s"analyzeTerQ processing '$q' -> $terms")
        terms match {
          case None => Left(s"Error tokenizing TermQ during analyzeTermQ: $q")
          case Some(ts) =>
            ts match {
              case NonEmptyList(head, Nil) => Right(Query.TermQ(head))
              case terms => Right(Query.Group(terms.map(Query.TermQ.apply)))
            }
        }
      case q: Query.FieldQ => Left(s"Oops, nested field query?: $q")
      case q: Query.AndQ =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.AndQ(qs))
      case q: Query.OrQ =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.OrQ(qs))
      case q: Query.Group =>
        q.qs.traverse(qq => analyzeTermQ(a, qq)).map(qs => Query.Group(qs))
      case q: Query.NotQ =>
        analyzeTermQ(a, q.q).map(qs => Query.NotQ(qs))
      case q: Query.UnaryMinus =>
        analyzeTermQ(a, q.q).map(qs => Query.UnaryMinus(qs))
      case q: Query.UnaryPlus =>
        analyzeTermQ(a, q.q).map(qs => Query.UnaryPlus(qs))
      case q: Query.ProximityQ => Right(q)
      case q: Query.PrefixTerm => Right(q)
      case q: Query.PhraseQ => Right(q)
      case q: Query.RangeQ => Right(q)
      case q: Query.FuzzyTerm => Right(q)
    }

  private def analyzeQ(query: Query): Either[String, Query] =
    query match {
      case Query.TermQ(q) =>
        // TODO This is a hack, the Lucille parser tokenizes on white space currently
        // We really want to pass in our tokenizer somehow
        val qs: List[String] = analyzers(defaultField).tokenize(q)
        NonEmptyList.fromList(qs) match {
          case None => Left(s"Query analysis error, no terms found after tokenizing $query")
          case Some(qs) =>
            if (qs.length == 1) Right(Query.TermQ(qs.head))
            else
              Left(
                s"Query analysis error, TermQ tokenized into multiple terms, this should be supported, but isn't yet"
              )
        }
      case Query.FieldQ(fn, q) =>
        analyzers.get(fn) match {
          case None => Left(s"Query analysis error, field $fn is not supported in query $query")
          case Some(a) => analyzeTermQ(a, q).map(qq => Query.FieldQ(fn, qq))
        }
      case Query.PhraseQ(q) =>
        // TODO This is also a hack, we don't really support phrase queries yet
        // But if the phrase contains a single term, we can do it!
        val qs: List[String] = analyzers(defaultField).tokenize(q)
        NonEmptyList.fromList(qs) match {
          case None => Left(s"Query analysis error, no terms found after tokenizing $query")
          case Some(qs) =>
            if (qs.length == 1) Right(Query.PhraseQ(qs.head))
            else
              Left(
                s"Query analysis error, PhraseQ tokenized into multiple terms, this should be supported, but isn't yet"
              )
        }
      case q: Query.AndQ => q.qs.traverse(analyzeQ).map(Query.AndQ.apply)
      case q: Query.OrQ => q.qs.traverse(analyzeQ).map(Query.OrQ.apply)
      case q: Query.Group => q.qs.traverse(analyzeQ).map(Query.Group.apply)
      case q: Query.NotQ => analyzeQ(q.q).map(Query.NotQ.apply)
      case q: Query.UnaryMinus => analyzeQ(q.q).map(Query.UnaryMinus.apply)
      case q: Query.UnaryPlus => analyzeQ(q.q).map(Query.UnaryPlus.apply)
      case q: Query.ProximityQ => Right(q)
      case q: Query.PrefixTerm => Right(q)
      case q: Query.RangeQ => Right(q)
      case q: Query.FuzzyTerm => Right(q)
    }

  def parse(queryString: String): Either[String, NonEmptyList[Query]] = {
    val q: Either[String, NonEmptyList[Query]] =
      Parser
        .parseQ(queryString)
        .leftMap(err => s"Parse error before query analysis, err: $err")
    q.flatMap(qs => qs.traverse(analyzeQ))
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
