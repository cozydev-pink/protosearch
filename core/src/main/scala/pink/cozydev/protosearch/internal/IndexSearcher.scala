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

import cats.data.NonEmptyList
import pink.cozydev.lucille.{Query, TermQuery}
import pink.cozydev.protosearch._

import java.util.regex.PatternSyntaxException

sealed abstract class IndexSearcher {
  def search(q: Query): Either[String, Set[Int]]

  def search(q: NonEmptyList[Query]): Either[String, Set[Int]]
}
object IndexSearcher {

  def apply(multiIndex: MultiIndex, defaultOR: Boolean): IndexSearcher =
    new MultiIndexSearcher(multiIndex, defaultOR)

  def apply(index: Index, defaultOR: Boolean = true): IndexSearcher =
    new SingleIndexSearcher(index, defaultOR)

  private class MultiIndexSearcher(index: MultiIndex, defaultOR: Boolean) extends IndexSearcher {

    private val defaultIndex = index.indexes(index.schema.defaultField)
    private lazy val allDocs: Set[Int] = Range(0, defaultIndex.numDocs).toSet
    private val defaultCombine =
      if (defaultOR)
        (sets: NonEmptyList[Set[Int]]) => IndexSearcher.unionSets(sets)
      else
        (sets: NonEmptyList[Set[Int]]) => IndexSearcher.intersectSets(sets)

    def search(q: NonEmptyList[Query]): Either[String, Set[Int]] =
      q.traverse(q => search(q)).map(defaultCombine)

    def search(q: Query): Either[String, Set[Int]] =
      q match {
        case Query.Or(qs) => qs.traverse(search).map(IndexSearcher.unionSets)
        case Query.And(qs) => qs.traverse(search).map(IndexSearcher.intersectSets)
        case Query.Not(q) => search(q).map(matches => allDocs -- matches)
        case Query.Group(q) => search(q)
        case Query.Field(f, q) =>
          index.indexes
            .get(f)
            .toRight(s"unsupported field $f")
            .flatMap(idx => new SingleIndexSearcher(idx, defaultOR).search(q))
        case qt: TermQuery => new SingleIndexSearcher(defaultIndex, defaultOR).search(qt)
        case q: Query.UnaryMinus => Left(s"Unsupported UnaryMinus in query: $q")
        case q: Query.UnaryPlus => Left(s"Unsupported UnaryPlus in query: $q")
        case q: Query.MinimumMatch => Left(s"Unsupported MinimumMatch in query: $q")
        case q: Query.Boost => Left(s"Unsupported Boost in query: $q")
      }
  }

  private class SingleIndexSearcher(index: Index, defaultOR: Boolean) extends IndexSearcher {

    private lazy val allDocs: Set[Int] = Range(0, index.numDocs).toSet
    private val defaultCombine =
      if (defaultOR)
        (sets: NonEmptyList[Set[Int]]) => IndexSearcher.unionSets(sets)
      else
        (sets: NonEmptyList[Set[Int]]) => IndexSearcher.intersectSets(sets)

    def search(q: NonEmptyList[Query]): Either[String, Set[Int]] =
      q.traverse(q => search(q)).map(defaultCombine)

    def search(q: Query): Either[String, Set[Int]] =
      q match {
        case Query.Term(q) => Right(index.docsWithTerm(q).toSet)
        case Query.Prefix(p) => Right(index.docsForPrefix(p).toSet)
        case q: Query.TermRange => rangeSearch(index, q)
        case q: Query.Phrase => phraseSearch(index, q)
        case Query.Or(qs) => qs.traverse(search).map(IndexSearcher.unionSets)
        case Query.And(qs) => qs.traverse(search).map(IndexSearcher.intersectSets)
        case Query.Not(q) => search(q).map(matches => allDocs -- matches)
        case Query.Group(q) => search(q)
        case Query.Field(fn, q) =>
          Left(s"Nested field queries not supported. Cannot query field '$fn' with q: $q")
        case q: Query.UnaryMinus => Left(s"Unsupported UnaryMinus in BooleanRetrieval: $q")
        case q: Query.UnaryPlus => Left(s"Unsupported UnaryPlus in BooleanRetrieval: $q")
        case q: Query.Proximity => Left(s"Unsupported Proximity in BooleanRetrieval: $q")
        case q: Query.Fuzzy => Left(s"Unsupported Fuzzy in BooleanRetrieval: $q")
        case q: Query.TermRegex => regexSearch(q)
        case q: Query.MinimumMatch => Left(s"Unsupported MinimumMatch in BooleanRetrieval: $q")
        case q: Query.Boost => Left(s"Unsupported Boost in BooleanRetrieval: $q")
        case q: Query.WildCard => Left(s"Unsupported WildCard in BooleanRetrieval: $q")
      }

    private def phraseSearch(index: Index, q: Query.Phrase): Either[String, Set[Int]] =
      index match {
        case pindex: PositionalIndex =>
          PositionalIter.exact(pindex, q) match {
            case None => Right(Set.empty)
            case Some(pIter) => Right(pIter.takeWhile(_ > -1).toSet)
          }
        case idx: Index =>
          // Optimistic phrase query handling for single term only
          val resultSet = idx.docsWithTerm(q.str).toSet
          if (resultSet.nonEmpty) Right(resultSet)
          else
            Left(s"Phrase queries require position data, which we don't have yet. q: $q")
      }

    private def rangeSearch(index: Index, q: Query.TermRange): Either[String, Set[Int]] =
      q match {
        case Query.TermRange(left, right, _, _) =>
          (left, right) match {
            case (Some(l), Some(r)) =>
              // TODO handle inclusive / exclusive
              // TODO optionality
              Right(index.docsForRange(l, r).toSet)
            case _ => Left("Unsupport TermRange error?")
          }
      }

    private def regexSearch(q: Query.TermRegex): Either[String, Set[Int]] = {
      val regex =
        try
          q.str.r
        catch {
          case _: PatternSyntaxException => return Left(s"Invalid regex query $q")
        }
      val terms = index.termDict.termsForRegex(regex)
      Right(
        terms
          .flatMap(m => index.docsWithTerm(m))
          .toSet
      )
    }
  }

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
