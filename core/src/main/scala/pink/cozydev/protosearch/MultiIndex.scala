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

import scala.collection.mutable.ListBuffer
import pink.cozydev.lucille.Query
import cats.data.NonEmptyList

case class MultiIndex(
    indexes: Map[String, TermIndexArray],
    numDocs: Int,
    defaultField: String,
    defaultOR: Boolean = true,
) {

  private lazy val allDocs: Set[Int] = Set.from(Range(0, numDocs))

  def search(q: NonEmptyList[Query]): Either[String, List[Int]] = {
    val docs = q.traverse(q => booleanModel(q)).map(defaultCombine)
    docs.map(_.toList.sorted)
  }

  def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      // TODO use analyzer on TermQ
      case Query.TermQ(q) => Right(indexes(defaultField).docsWithTermSet(q))
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(intersectSets)
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(unionSets)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.NotQ(q) => booleanModel(q).map(matches => allDocs.removedAll(matches))
      case Query.FieldQ(f, q) =>
        indexes.get(f).toRight(s"unsupported field $f").flatMap { index =>
          BooleanQuery(index, defaultOR).search(q).map(xs => xs.map(_._1).toSet)
        }
      case _: Query.PhraseQ => Left("Phrase queries require position data, which we don't have yet")
      case _: Query.ProximityQ => Left("Unsupported query type")
      case _: Query.PrefixTerm => Left("Unsupported query type")
      case _: Query.FuzzyTerm => Left("Unsupported query type")
      // Should normalize before we get here?
      case _: Query.UnaryPlus => Left("Unsupported query type")
      case _: Query.UnaryMinus => Left("Unsupported query type")
      case _: Query.RangeQ =>
        BooleanQuery(indexes(defaultField), defaultOR).search(q).map(xs => xs.map(_._1).toSet)
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) unionSets(sets) else intersectSets(sets)

  private def intersectSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
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

  private def unionSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
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
object MultiIndex {
  private case class Bldr[A](
      name: String,
      getter: A => String,
      tokenizer: String => Vector[String],
      acc: ListBuffer[Vector[String]],
  )

  def apply[A](
      head: (String, A => String, String => Vector[String]),
      tail: (String, A => String, String => Vector[String])*
  ): Vector[A] => MultiIndex = {

    val bldrs = (head :: tail.toList).map { case (name, getter, tokenizer) =>
      Bldr(name, getter, tokenizer, ListBuffer.empty)
    }

    var numDocs = 0
    docs => {
      docs.foreach { doc =>
        numDocs += 1
        bldrs.foreach { bldr =>
          bldr.acc.addOne(bldr.tokenizer(bldr.getter(doc)))
        }
      }
      // TODO let's delay defining the default field even further
      // Also, let's make it optional, with no field meaning all fields?
      MultiIndex(
        bldrs.map(bldr => (bldr.name, TermIndexArray(bldr.acc.toVector))).toMap,
        numDocs,
        "title",
      )
    }
  }
}
