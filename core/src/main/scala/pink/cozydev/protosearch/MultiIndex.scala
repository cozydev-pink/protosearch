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
    defaultField: String,
    defaultOR: Boolean = true,
) {

  def search(q: NonEmptyList[Query]): Either[String, List[Int]] = {
    val docs = q.traverse(q => booleanModel(q)).map(defaultCombine)
    docs.map(_.toList.sorted)
  }

  private val defaultIndex = indexes(defaultField)
  private val defaultBooleanQ =
    BooleanQuery(indexes(defaultField), defaultOR)

  private lazy val allDocs: Set[Int] = Set.from(Range(0, defaultIndex.numDocs))

  def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(BooleanQuery.intersectSets)
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(BooleanQuery.unionSets)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.NotQ(q) => booleanModel(q).map(matches => allDocs.removedAll(matches))
      case Query.FieldQ(f, q) =>
        indexes.get(f).toRight(s"unsupported field $f").flatMap { index =>
          BooleanQuery(index, defaultOR).search(q).map(xs => xs.map(_._1).toSet)
        }
      case _ =>
        defaultBooleanQ
          .search(q)
          .map(xs => xs.map(_._1).toSet)
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) BooleanQuery.unionSets(sets) else BooleanQuery.intersectSets(sets)

}
object MultiIndex {
  private case class Bldr[A](
      name: String,
      getter: A => String,
      analyzer: Analyzer,
      acc: ListBuffer[Vector[String]],
  )

  def apply[A](
      defaultField: String,
      head: (String, A => String, Analyzer),
      tail: (String, A => String, Analyzer)*
  ): Vector[A] => MultiIndex = {

    val bldrs = (head :: tail.toList).map { case (name, getter, tokenizer) =>
      Bldr(name, getter, tokenizer, ListBuffer.empty)
    }

    docs => {
      docs.foreach { doc =>
        bldrs.foreach { bldr =>
          bldr.acc.addOne(bldr.analyzer.tokenize(bldr.getter(doc)))
        }
      }
      // TODO let's delay defining the default field even further
      // Also, let's make it optional, with no field meaning all fields?
      MultiIndex(
        bldrs.map(bldr => (bldr.name, TermIndexArray(bldr.acc.toVector))).toMap,
        defaultField,
      )
    }
  }
}
