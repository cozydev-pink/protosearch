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

case class BooleanRetrieval(index: Index, defaultOR: Boolean = true) {

  private lazy val allDocs: Set[Int] = Set.from(Range(0, index.numDocs))

  def search(q: Query): Either[String, Set[Int]] = {
    val docs = booleanModel(q)
    docs
  }

  private def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.TermQ(q) => Right(index.docsWithTermSet(q))
      case Query.PrefixTerm(p) => Right(index.docsForPrefix(p))
      case q: Query.RangeQ => rangeSearch(q)
      case q: Query.PhraseQ => phraseSearch(q)
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.unionSets)
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.intersectSets)
      case Query.NotQ(q) => booleanModel(q).map(matches => allDocs.removedAll(matches))
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.FieldQ(fn, q) =>
        Left(s"Nested field queries not supported. Cannot query field '$fn' with q: $q")
      case q: Query.UnaryMinus => Left(s"Unsupported UnaryMinus in BooleanRetrieval: $q")
      case q: Query.UnaryPlus => Left(s"Unsupported UnaryPlus in BooleanRetrieval: $q")
      case q: Query.ProximityQ => Left(s"Unsupported ProximityQ in BooleanRetrieval: $q")
      case q: Query.FuzzyTerm => Left(s"Unsupported FuzzyTerm in BooleanRetrieval: $q")
    }

  private def phraseSearch(q: Query.PhraseQ): Either[String, Set[Int]] = {
    // Optimistic phrase query handling for single term only
    val resultSet = index.docsWithTermSet(q.q)
    if (resultSet.nonEmpty) Right(resultSet)
    else
      Left(s"Phrase queries require position data, which we don't have yet. q: $q")
  }

  private def rangeSearch(q: Query.RangeQ): Either[String, Set[Int]] =
    q match {
      case Query.RangeQ(left, right, _, _) =>
        (left, right) match {
          case (Some(l), Some(r)) =>
            // TODO handle inclusive / exclusive
            // TODO optionality
            Right(index.docsForRange(l, r))
          case _ => Left("Unsupport RangeQ error?")
        }
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) BooleanRetrieval.unionSets(sets) else BooleanRetrieval.intersectSets(sets)

}
object BooleanRetrieval {

  def intersectSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (sets.size == 1) sets.head
    else {
      val setList = sets.tail
      var s = sets.head
      var i = 0
      while (i < setList.size) {
        s = s.intersect(setList(i))
        i += 1
      }
      s
    }

  def unionSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (sets.size == 1) sets.head
    else {
      val setList = sets.tail
      var s = sets.head
      var i = 0
      while (i < setList.size) {
        s = s.union(setList(i))
        i += 1
      }
      s
    }
}
