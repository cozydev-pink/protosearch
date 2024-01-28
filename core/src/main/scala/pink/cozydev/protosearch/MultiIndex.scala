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

case class MultiIndex(
    indexes: Map[String, FrequencyIndex],
    defaultField: String,
    defaultOR: Boolean = true,
    fields: Map[String, Array[String]],
) {

  def search(q: NonEmptyList[Query]): Either[String, List[Int]] = {
    val docs = q.traverse(q => booleanModel(q)).map(defaultCombine)
    docs.map(_.toList.sorted)
  }

  def searchMap(q: NonEmptyList[Query]): Either[String, List[(Int, Map[String, String])]] = {
    val docs = q.traverse(q => booleanModel(q)).map(defaultCombine)
    val lstb = List.newBuilder[(Int, Map[String, String])]
    docs.map(_.foreach { d =>
      lstb += d -> fields.map { case (k, v) => (k, v(d)) }
    })
    docs.map(_ => lstb.result())
  }

  private val defaultIndex = indexes(defaultField)
  private val defaultBooleanQ =
    IndexSearcher(indexes(defaultField), defaultOR)

  private lazy val allDocs: Set[Int] = Range(0, defaultIndex.numDocs).toSet

  private def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.Or(qs) => qs.traverse(booleanModel).map(IndexSearcher.unionSets)
      case Query.And(qs) => qs.traverse(booleanModel).map(IndexSearcher.intersectSets)
      case Query.Not(q) => booleanModel(q).map(matches => allDocs -- matches)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.Field(f, q) =>
        indexes.get(f).toRight(s"unsupported field $f").flatMap { index =>
          IndexSearcher(index, defaultOR).search(q)
        }
      case _ => defaultBooleanQ.search(q)
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) IndexSearcher.unionSets(sets) else IndexSearcher.intersectSets(sets)

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
      head: (Field, A => String),
      tail: (Field, A => String)*
  ): List[A] => MultiIndex = {

    val bldrs = (head :: tail.toList).map { case (field, getter) =>
      Bldr(field.name, getter, field.analyzer, ListBuffer.empty)
    }

    val storage = (head :: tail.toList).map(fg => fg._1.name -> ListBuffer.empty[String]).toMap

    docs => {
      docs.foreach { doc =>
        bldrs.foreach { bldr =>
          val value = bldr.getter(doc)
          storage(bldr.name) += value
          bldr.acc += (bldr.analyzer.tokenize(value))
        }
      }
      // TODO let's delay defining the default field even further
      // Also, let's make it optional, with no field meaning all fields?
      new MultiIndex(
        indexes = bldrs.map(bldr => (bldr.name, FrequencyIndex(bldr.acc.toList))).toMap,
        defaultField = defaultField,
        defaultOR = true,
        fields = storage.map { case (k, v) => k -> v.toArray },
      )
    }
  }

  import pink.cozydev.protosearch.codecs.IndexCodecs

  val codec: Codec[MultiIndex] = {

    val indexes: Codec[Map[String, FrequencyIndex]] =
      codecs
        .listOfN(codecs.vint, (codecs.utf8_32 :: FrequencyIndex.codec).as[(String, FrequencyIndex)])
        .xmap(_.toMap, _.toList)
    val defaultField: Codec[String] = codecs.utf8_32.withContext("defaultField")
    val defaultOr: Codec[Boolean] = codecs.bool.withContext("defaultOr")

    val fieldStrings: Codec[Array[String]] =
      IndexCodecs.arrayOfN(codecs.vint, codecs.variableSizeBytes(codecs.vint, codecs.utf8))

    val fields: Codec[Map[String, Array[String]]] =
      codecs
        .listOfN(codecs.vint, (codecs.utf8_32 :: fieldStrings).as[(String, Array[String])])
        .xmap(_.toMap, _.toList)

    val multiIndex: Codec[MultiIndex] =
      (indexes :: defaultField :: defaultOr :: fields)
        .as[(Map[String, FrequencyIndex], String, Boolean, Map[String, Array[String]])]
        .xmap(
          { case (in, dF, dOr, fs) => MultiIndex.apply(in, dF, dOr, fs) },
          mi => (mi.indexes, mi.defaultField, mi.defaultOR, mi.fields),
        )
    multiIndex
  }
}
