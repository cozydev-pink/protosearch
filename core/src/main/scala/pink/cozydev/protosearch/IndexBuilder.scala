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

final case class IndexBuilder[A] private (
    fieldAndGetters: List[(Field, A => String)],
    defaultField: String,
) {
  val schema = Schema(fieldAndGetters.head._1, fieldAndGetters.tail.map(_._1))

  def fromList(docs: List[A]): MultiIndex = {
    val fields = fieldAndGetters.map(_._1)
    val buffers: Map[String, ListBuffer[List[String]]] =
      fields.map(k => (k.name, ListBuffer.empty[List[String]])).toMap
    val storage: Map[String, ListBuffer[String]] =
      fields.map(k => (k.name, ListBuffer.empty[String])).toMap

    docs.foreach { doc =>
      fieldAndGetters.foreach { case (field, getter) =>
        val fieldValue = getter(doc)
        storage(field.name) += fieldValue
        buffers(field.name) += field.analyzer.tokenize(fieldValue)
      }
    }
    val indexes = fields.map { f =>
      val idx =
        if (f.positions) PositionalIndex(buffers(f.name).toList)
        else FrequencyIndex(buffers(f.name).toList)
      (f.name, idx)
    }.toMap
    new MultiIndex(
      indexes = indexes,
      schema = schema,
      fields = storage.map { case (k, v) => (k, v.toArray) },
    )
  }
}
object IndexBuilder {
  def of[A](
      defaultField: (Field, A => String),
      fields: (Field, A => String)*
  ): IndexBuilder[A] =
    IndexBuilder(defaultField :: fields.toList, defaultField._1.name)
}
