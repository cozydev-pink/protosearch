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

package pink.cozydev.protosearch.internal

import pink.cozydev.protosearch.Index
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.Query._
import pink.cozydev.protosearch.PositionalIndex
import pink.cozydev.protosearch.MultiIndex
import pink.cozydev.lucille.TermQuery
import java.util.regex.PatternSyntaxException

abstract class IndexSearcher {
  def search(q: Query): Either[String, Set[Int]]
}
object QueryIteratorSearch {
  def apply(multiIndex: MultiIndex): IndexSearcher = {
    val doer = new MultiIndexQueryIteratorSearcher(multiIndex)
    new IndexSearcher {
      def search(q: Query): Either[String, Set[Int]] = doer.doit(q).map(_.docs.toSet)

    }
  }
  def apply(index: Index): IndexSearcher = {
    val doer = new SingleIndexQueryIteratorSearcher(index)
    new IndexSearcher {
      def search(q: Query): Either[String, Set[Int]] = doer.doit(q).map(_.docs.toSet)

    }
  }

  private class MultiIndexQueryIteratorSearcher(index: MultiIndex) {
    private val defaultIndex = index.indexes(index.schema.defaultField)

    def doit(q: Query): Either[String, QueryIterator] = q match {
      case qt: TermQuery => new SingleIndexQueryIteratorSearcher(defaultIndex).doit(qt)
      case Query.Field(f, q) =>
        index.indexes
          .get(f)
          .toRight(s"Unsupported field: '$f'")
          .flatMap(idx => new SingleIndexQueryIteratorSearcher(idx).doit(q))
      case Query.Group(q) => doit(q)
      case Query.And(qs) => qs.traverse(doit).map(qis => AndIter(qis.toList))
      case Query.Or(qs) => qs.traverse(doit).map(qis => OrQueryIterator(qis.toList, 1))
      case Query.Boost(q, boost) => doit(q).map(qi => new BoostQueryIterator(qi, boost))
      case Query.MinimumMatch(qs, num) =>
        qs.traverse(doit).map(qis => OrQueryIterator(qis.toList, num))
      case q: Query.Not =>
        doit(q.q).map(qi => new NotQueryIterator(qi, defaultIndex.numDocs))
      case _: Query.UnaryMinus => ???
      case _: Query.UnaryPlus => ???
    }
  }

  private class SingleIndexQueryIteratorSearcher(index: Index) {
    def doit(query: Query): Either[String, QueryIterator] = query match {
      case q: Query.Term => Right(index.docsWithTermIter(q.str))
      case q: Query.Prefix => Right(index.docsForPrefixIter(q.str))
      case q: Query.TermRange => rangeSearch(q)
      case q: Query.TermRegex => regexSearch(q)
      case q: Query.Phrase =>
        index match {
          case indx: PositionalIndex =>
            PositionalIter.exact(indx, q) match {
              case Some(pi) => Right(pi)
              case None => Left(s"Some terms in phrase '$q' could not be found in index")
            }
          case _ => Left("Index does not support phrase queries")
        }
      case q: Query.Field =>
        Left(s"Unsupported nested field query: $q")
      case Query.Group(q) => doit(q)
      case Query.And(qs) => qs.traverse(doit).map(qis => AndIter(qis.toList))
      case Query.Or(qs) => qs.traverse(doit).map(qis => OrQueryIterator(qis.toList, 1))
      case q: Query.Not =>
        doit(q.q).map(qi => new NotQueryIterator(qi, index.numDocs))
      case Query.Boost(q, boost) => doit(q).map(qi => new BoostQueryIterator(qi, boost))
      case MinimumMatch(qs, num) =>
        qs.traverse(doit).map(qis => OrQueryIterator(qis.toList, num))
      case _: Query.UnaryMinus => ???
      case _: Query.UnaryPlus => ???
      case _: Query.Proximity => ???
      case _: Query.Fuzzy => ???
      case _: Query.WildCard => ???
    }

    private def rangeSearch(q: Query.TermRange): Either[String, QueryIterator] =
      q match {
        case Query.TermRange(left, right, _, _) =>
          (left, right) match {
            case (Some(l), Some(r)) =>
              // TODO handle inclusive / exclusive
              // TODO optionality
              Right(index.docsForRangeIter(l, r))
            case _ => Left("Unsupported TermRange error?")
          }
      }

    private def regexSearch(q: Query.TermRegex): Either[String, QueryIterator] = {
      val regex =
        try
          q.str.r.pattern
        catch {
          case _: PatternSyntaxException => return Left(s"Invalid regex query $q")
        }
      Right(index.docsForRegexIter(regex))
    }
  }

}
