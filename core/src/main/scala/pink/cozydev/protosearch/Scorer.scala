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
import cats.syntax.all.*
import pink.cozydev.lucille.Query

import scala.collection.mutable.HashMap
import pink.cozydev.lucille.MultiQuery
import pink.cozydev.protosearch.internal.PositionalIter

import java.util.regex.PatternSyntaxException

case class Scorer(index: MultiIndex, defaultOR: Boolean = true) {

  private val defaultIdx: Index = index.indexes(index.schema.defaultField)

  def score(qs: Query, docs: Set[Int]): Either[String, List[(Int, Double)]] = {
    def accScore(
        idx: Index,
        queries: NonEmptyList[Query],
    ): Either[String, NonEmptyList[Map[Int, Double]]] =
      queries.flatTraverse {
        case Query.Term(t) => Right(NonEmptyList.one(idx.scoreTFIDF(docs, t).toMap))
        case q: Query.Prefix => prefixScore(idx, docs, q)
        case q: Query.TermRange => rangeScore(idx, docs, q)
        case q: Query.Phrase => phraseScore(idx, docs, q)
        case Query.Or(qs) => accScore(idx, qs)
        case Query.And(qs) => accScore(idx, qs)
        case Query.Not(_) => Right(NonEmptyList.one(Map.empty[Int, Double]))
        case Query.Group(qs) => accScore(idx, qs)
        case Query.Field(fn, q) =>
          index.indexes.get(fn) match {
            case None => Left(s"Field not found")
            case Some(newIndex) => accScore(newIndex, NonEmptyList.one(q))
          }
        case q: MultiQuery => accScore(idx, q.qs)
        case Query.UnaryMinus(_) => Right(NonEmptyList.one(Map.empty[Int, Double]))
        case Query.UnaryPlus(q) => accScore(idx, NonEmptyList.one(q))
        case q: Query.Proximity => Left(s"Unsupported Proximity encountered in Scorer: $q")
        case q: Query.Fuzzy => Left(s"Unsupported Fuzzy encountered in Scorer: $q")
        case q: Query.TermRegex => regexScore(idx, docs, q)
        case q: Query.MinimumMatch => Left(s"Unsupported MinimumMatch in Scorer: $q")
        case q: Query.Boost => Left(s"Unsupported Boost in Scorer: $q")
      }
    accScore(defaultIdx, NonEmptyList.one(qs)).map(combineMaps)
  }

  private def phraseScore(
      idx: Index,
      docs: Set[Int],
      q: Query.Phrase,
  ): Either[String, NonEmptyList[Map[Int, Double]]] =
    idx match {
      case pindex: PositionalIndex =>
        PositionalIter.exact(pindex, q) match {
          case None => Right(NonEmptyList.one(Map.empty[Int, Double]))
          case Some(pIter) =>
            // TODO this is a total hack, and not how phrase scoring works
            val scoreMap = pIter.takeWhile(_ > -1).map(docId => (docId, 1.0)).toMap
            Right(NonEmptyList.one(scoreMap))
        }
      case idx: Index =>
        // Optimistic phrase query handling for single term only
        Right(NonEmptyList.one(idx.scoreTFIDF(docs, q.str).toMap))
    }

  private def prefixScore(
      idx: Index,
      docs: Set[Int],
      q: Query.Prefix,
  ): Either[String, NonEmptyList[Map[Int, Double]]] =
    NonEmptyList.fromList(idx.termDict.termsForPrefix(q.str)) match {
      case None => Right(NonEmptyList.one(Map.empty[Int, Double]))
      case Some(terms) => Right(terms.map(t => idx.scoreTFIDF(docs, t).toMap))
    }

  private def rangeScore(
      idx: Index,
      docs: Set[Int],
      q: Query.TermRange,
  ): Either[String, NonEmptyList[Map[Int, Double]]] =
    (q.lower, q.upper) match {
      case (Some(l), Some(r)) =>
        // TODO handle inclusive / exclusive, optionality
        NonEmptyList
          .fromList(idx.termDict.termsForRange(l, r))
          .toRight(
            s"No terms found while processing TermRange: $q"
          )
          .map(ts => ts.map(t => idx.scoreTFIDF(docs, t).toMap))
      case _ => Left(s"Unsupported TermRange error: $q")
    }

  private def regexScore(
      idx: Index,
      docs: Set[Int],
      q: Query.TermRegex,
  ): Either[String, NonEmptyList[Map[Int, Double]]] = {
    val regex =
      try
        q.str.r
      catch {
        case _: PatternSyntaxException => return Left(s"Invalid regex query $q")
      }

    NonEmptyList.fromList(idx.termDict.termsForRegex(regex)) match {
      case None => Right(NonEmptyList.one(Map.empty[Int, Double]))
      case Some(terms) => Right(terms.map(idx.scoreTFIDF(docs, _).toMap))
    }
  }

  private def combineMaps(ms: NonEmptyList[Map[Int, Double]]): List[(Int, Double)] = {
    val mb = HashMap.empty ++ ms.head
    ms.tail.foreach(m1 =>
      m1.foreach { case (k: Int, v: Double) => mb.update(k, v + mb.getOrElse(k, 0.0)) }
    )
    mb.toList.sortBy(idScore => (-idScore._2, idScore._1))
  }
}
