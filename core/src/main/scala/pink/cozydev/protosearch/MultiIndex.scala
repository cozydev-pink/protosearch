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

import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.protosearch.BooleanRetrieval

case class MultiIndex(
    indexes: Map[String, Index],
    defaultField: String,
    defaultOR: Boolean = true,
) {

  def search(q: NonEmptyList[Query]): Either[String, List[Int]] = {
    val docs = q.traverse(q => booleanModel(q)).map(defaultCombine)
    docs.map(_.toList.sorted)
  }

  private val defaultIndex = indexes(defaultField)
  private val defaultBooleanQ =
    BooleanRetrieval(indexes(defaultField), defaultOR)

  private lazy val allDocs: Set[Int] = Set.from(Range(0, defaultIndex.numDocs))

  def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.intersectSets)
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(BooleanRetrieval.unionSets)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.NotQ(q) => booleanModel(q).map(matches => allDocs.removedAll(matches))
      case Query.FieldQ(f, q) =>
        indexes.get(f).toRight(s"unsupported field $f").flatMap { index =>
          BooleanRetrieval(index, defaultOR).search(q)
        }
      case _ => defaultBooleanQ.search(q)
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) BooleanRetrieval.unionSets(sets) else BooleanRetrieval.intersectSets(sets)

}
object MultiIndex {
  import scodec.{Codec, codecs}

  private case class Bldr[A](
      name: String,
      getter: A => String,
      analyzer: Analyzer,
      acc: ListBuffer[List[String]],
  )

  def apply[A](
      defaultField: String,
      head: (String, A => String, Analyzer),
      tail: (String, A => String, Analyzer)*
  ): List[A] => MultiIndex = {

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
        bldrs.map(bldr => (bldr.name, Index(bldr.acc.toList))).toMap,
        defaultField,
      )
    }
  }

  val codec: Codec[MultiIndex] = {

    val indexes: Codec[Map[String, Index]] =
      codecs
        .listOfN(codecs.vint, (codecs.utf8_32 :: Index.codec).as[(String, Index)])
        .xmap(_.toMap, _.toList)
    val defaultField: Codec[String] = codecs.utf8_32.withContext("defaultField")
    val defaultOr: Codec[Boolean] = codecs.bool.withContext("defaultOr")
    val multiIndex: Codec[MultiIndex] =
      (indexes :: defaultField :: defaultOr)
        .as[(Map[String, Index], String, Boolean)]
        .xmap(
          { case (in, dF, dOr) => MultiIndex.apply(in, dF, dOr) },
          mi => (mi.indexes, mi.defaultField, mi.defaultOR),
        )
    multiIndex
  }
}
