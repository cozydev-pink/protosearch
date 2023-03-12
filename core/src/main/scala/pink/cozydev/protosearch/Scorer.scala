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
import scala.collection.mutable.{HashMap => MMap}

case class Scorer(index: MultiIndex, defaultOR: Boolean = true) {

  def score(qs: NonEmptyList[Query], docs: Set[Int]): Either[String, List[(Int, Double)]] = {
    // TODO unsafe
    val defaultIdx: Index = index.indexes(index.defaultField)
    def accScore(
        idx: Index,
        queries: NonEmptyList[Query],
    ): Either[String, NonEmptyList[Map[Int, Double]]] =
      queries.flatTraverse {
        case Query.OrQ(qs) => accScore(idx, qs)
        case Query.AndQ(qs) => accScore(idx, qs)
        case Query.TermQ(t) => Right(NonEmptyList.one(idx.scoreTFIDF(docs, t).toMap))
        case Query.Group(qs) => accScore(idx, qs)
        case Query.NotQ(_) => Right(NonEmptyList.one(Map.empty[Int, Double]))
        case Query.RangeQ(left, right, _, _) =>
          (left, right) match {
            case (Some(l), Some(r)) =>
              // TODO handle inclusive / exclusive
              // TODO optionality
              // TODO left might also require special handling
              NonEmptyList
                .fromList(idx.termsForRange(l, r))
                .toRight(
                  s"No terms found while processing RangeQ: [$left, $right]"
                )
                .map(ts => ts.map(t => idx.scoreTFIDF(docs, t).toMap))
            case _ => Left("Unsupport RangeQ error?")
          }
        case Query.PhraseQ(p) =>
          // TODO Hack, only works for single term phrase
          Right(NonEmptyList.one(idx.scoreTFIDF(docs, p).toMap))
        case Query.UnaryMinus(_) => Right(NonEmptyList.one(Map.empty[Int, Double]))
        case Query.UnaryPlus(q) => accScore(idx, NonEmptyList.one(q))
        case Query.FieldQ(fn, q) =>
          index.indexes.get(fn) match {
            case None => Left(s"Field not found")
            case Some(newIndex) => accScore(newIndex, NonEmptyList.one(q))
          }
        case q: Query.ProximityQ => Left(s"Unsupported ProximityQ encountered in Scorer: $q")
        case q: Query.PrefixTerm => Left(s"Unsupported PrefixTerm encountered in Scorer: $q")
        case q: Query.FuzzyTerm => Left(s"Unsupported FuzzyTerm encountered in Scorer: $q")
      }
    accScore(defaultIdx, qs).map(combineMaps)
  }

  private def combineMaps(ms: NonEmptyList[Map[Int, Double]]): List[(Int, Double)] = {
    val mb = MMap.from(ms.head)
    ms.tail.foreach(m1 =>
      m1.foreachEntry((k: Int, v: Double) => mb.update(k, v + mb.getOrElse(k, 0.0)))
    )
    mb.iterator.toList
  }
}
