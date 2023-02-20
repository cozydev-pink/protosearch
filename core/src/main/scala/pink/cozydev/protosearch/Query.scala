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

case class BooleanQuery(index: TermIndexArray, analyzer: Analyzer, defaultOR: Boolean = true) {

  private lazy val allDocs: Set[Int] = Set.from(Range(0, index.numDocs))

  def search(q: Query): Either[String, List[(Int, Double)]] = {
    val terms = onlyTerms(NonEmptyList.of(q))
    val docs = booleanModel(q)
    terms.flatMap(terms => docs.map(ds => scoreEm(terms, ds)))
  }

  def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.TermQ(q) =>
        val tokens = analyzer.tokenize(q)
        // TODO painful
        val sets = NonEmptyList.fromFoldable(tokens.map(t => index.docsWithTermSet(t)))
        sets.toRight(s"Error analyzing TermQ: $q").map(defaultCombine)
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(BooleanQuery.intersectSets)
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(BooleanQuery.unionSets)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.NotQ(q) => booleanModel(q).map(matches => allDocs.removedAll(matches))
      case _: Query.FieldQ => Left("We only have one implicit field currently")
      case _: Query.PhraseQ => Left("Phrase queries require position data, which we don't have yet")
      case _: Query.ProximityQ => Left("Unsupported query type")
      case _: Query.PrefixTerm => Left("Unsupported query type")
      case _: Query.FuzzyTerm => Left("Unsupported query type")
      case _: Query.UnaryPlus => Left("Unsupported query type")
      case _: Query.UnaryMinus => Left("Unsupported query type")
      case Query.RangeQ(left, right, _, _) =>
        (left, right) match {
          case (Some(l), Some(r)) =>
            // TODO handle inclusive / exclusive
            // TODO optionality
            val leftI = index.termDict.indexWhere(_ >= l)
            val rightI = index.termDict.indexWhere(_ >= r)
            Right(index.docsWithinRange(leftI, rightI))
          case _ => Left("Unsupport RangeQ error?")
        }
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) BooleanQuery.unionSets(sets) else BooleanQuery.intersectSets(sets)

  private def onlyTerms(queries: NonEmptyList[Query]): Either[String, NonEmptyList[String]] =
    queries.flatTraverse {
      case Query.OrQ(qs) => onlyTerms(qs)
      case Query.AndQ(qs) => onlyTerms(qs)
      case Query.TermQ(t) =>
        NonEmptyList
          .fromFoldable(analyzer.tokenize(t))
          .toRight(s"Could not extract any terms from TermQ: $t")
      case Query.Group(qs) => onlyTerms(qs)
      case Query.NotQ(q) => onlyTerms(NonEmptyList.of(q))
      case Query.RangeQ(left, right, _, _) =>
        (left, right) match {
          case (Some(l), Some(r)) =>
            // TODO handle inclusive / exclusive
            // TODO optionality
            val leftI = index.termDict.indexWhere(_ >= l)
            val rightI = index.termDict.indexWhere(_ >= r)
            NonEmptyList.fromList(index.termDict.slice(leftI, rightI).toList).toRight("No terms")
          case _ => Left("Unsupport RangeQ error?")
        }
      case x => Left(s"Sorry bucko, only term queries supported today, not $x")
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
object BooleanQuery {

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
