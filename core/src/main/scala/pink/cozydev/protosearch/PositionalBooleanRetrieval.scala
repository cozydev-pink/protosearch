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
import pink.cozydev.lucille.Query
import pink.cozydev.lucille.MultiQuery
import pink.cozydev.protosearch.internal.PhraseMeowMeow

case class PositionalBooleanRetrieval(index: PositionalIndex, defaultOR: Boolean = true) {

  private lazy val allDocs: Set[Int] = Range(0, index.numDocs).toSet

  def search(q: Query): Either[String, Set[Int]] = {
    val docs = booleanModel(q)
    docs
  }

  private def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.Term(q) => Right(index.docsWithTermSet(q))
      case Query.Prefix(p) => Right(index.docsForPrefix(p))
      case q: Query.TermRange => rangeSearch(q)
      case q: Query.Phrase => phraseSearch(q)
      case Query.Or(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.unionSets)
      case Query.And(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.intersectSets)
      case Query.Not(q) => booleanModel(q).map(matches => allDocs -- matches)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.Field(fn, q) =>
        Left(s"Nested field queries not supported. Cannot query field '$fn' with q: $q")
      case q: MultiQuery => q.qs.traverse(booleanModel).map(defaultCombine)
      case q: Query.UnaryMinus => Left(s"Unsupported UnaryMinus in BooleanRetrieval: $q")
      case q: Query.UnaryPlus => Left(s"Unsupported UnaryPlus in BooleanRetrieval: $q")
      case q: Query.Proximity => Left(s"Unsupported Proximity in BooleanRetrieval: $q")
      case q: Query.Fuzzy => Left(s"Unsupported Fuzzy in BooleanRetrieval: $q")
      case q: Query.TermRegex => Left(s"Unsupported Regex in BooleanRetrieval: $q")
      case q: Query.MinimumMatch => Left(s"Unsupported MinimumMatch in BooleanRetrieval: $q")
    }

  private def phraseSearch(q: Query.Phrase): Either[String, Set[Int]] = {
    // TODO Split phrase here for now. Probably should be done in Query Analysis
    val m = PhraseMeowMeow.exact(index, q)
    m match {
      case None => Left("NOOONE")
      case Some(mm) => Right(mm.takeWhile(_ > -1).toSet)
    }

  }

  private def rangeSearch(q: Query.TermRange): Either[String, Set[Int]] =
    q match {
      case Query.TermRange(left, right, _, _) =>
        (left, right) match {
          case (Some(l), Some(r)) =>
            // TODO handle inclusive / exclusive
            // TODO optionality
            Right(index.docsForRange(l, r))
          case _ => Left("Unsupport TermRange error?")
        }
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) BooleanRetrieval.unionSets(sets) else BooleanRetrieval.intersectSets(sets)

}
