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

case class Scorer(index: Index, defaultOR: Boolean = true) {

  def score(q: Query, docs: Set[Int]): Either[String, List[(Int, Double)]] = {
    val terms = onlyTerms(NonEmptyList.of(q))
    terms.map(terms => scoreEm(terms, docs))
  }

  private def onlyTerms(queries: NonEmptyList[Query]): Either[String, NonEmptyList[String]] =
    queries.flatTraverse {
      case Query.OrQ(qs) => onlyTerms(qs)
      case Query.AndQ(qs) => onlyTerms(qs)
      case Query.TermQ(t) => Right(NonEmptyList.of(t))
      case Query.Group(qs) => onlyTerms(qs)
      case Query.NotQ(q) => onlyTerms(NonEmptyList.of(q))
      case Query.RangeQ(left, right, _, _) =>
        (left, right) match {
          case (Some(l), Some(r)) =>
            // TODO handle inclusive / exclusive
            // TODO optionality
            // TODO left might also require special handling
            NonEmptyList
              .fromList(index.termsForRange(l, r))
              .toRight(
                s"No terms found while processing RangeQ: [$left, $right]"
              )
          case _ => Left("Unsupport RangeQ error?")
        }
      case Query.PhraseQ(qs) => Right(NonEmptyList.one(qs))
      case Query.UnaryMinus(q) => onlyTerms(NonEmptyList.one(q))
      case Query.UnaryPlus(q) => onlyTerms(NonEmptyList.one(q))
      case Query.FieldQ(_, q) => onlyTerms(NonEmptyList.one(q))
      case q: Query.ProximityQ => Left(s"Unsupported ProximityQ encountered in Scorer: $q")
      case q: Query.PrefixTerm => Left(s"Unsupported PrefixTerm encountered in Scorer: $q")
      case q: Query.FuzzyTerm => Left(s"Unsupported FuzzyTerm encountered in Scorer: $q")
    }

  private def scoreEm(
      terms: NonEmptyList[String],
      docs: Set[Int],
  ): List[(Int, Double)] =
    terms.toList
      .flatMap(t => index.scoreTFIDF(docs, t))
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList
}
