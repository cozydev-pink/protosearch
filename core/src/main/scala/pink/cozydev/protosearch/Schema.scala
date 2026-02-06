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

import pink.cozydev.protosearch.analysis.QueryAnalyzer

/** A Schema describes how the `Field`s of a document type will be used for search.
  *
  * @param fields The list of fields
  * @param defaultField The default field to use when one is not specified in a query
  * @param defaultOR Whether to use a default `OR` when combining queries
  */
final class Schema private (
    private val fields: List[Field],
    val defaultField: String,
    val defaultOR: Boolean = true,
) {
  val storedFields: Set[String] = {
    val bldr = Set.newBuilder[String]
    fields.foreach(f => if (f.stored) bldr += f.name)
    bldr.result()
  }

  def queryAnalyzer(defaultField: String): QueryAnalyzer = {
    val analyzers = fields.map(f => (f.name, f.analyzer))
    QueryAnalyzer(defaultField, analyzers.head, analyzers.tail: _*)
  }
}
object Schema {
  import scodec.Codec
  import scodec.codecs

  def apply[A](
      head: Field,
      tail: List[Field],
  ): Schema =
    new Schema(head :: tail, head.name)

  def of[A](
      head: Field,
      tail: Field*
  ): Schema =
    new Schema(head :: tail.toList, head.name)

  private val fields: Codec[List[Field]] = codecs.listOfN(codecs.vint, Field.codec)
  private val defaultField: Codec[String] = codecs.utf8_32.withContext("defaultField")
  private val defaultOR: Codec[Boolean] = codecs.bool.withContext("defaultOR")

  val codec: Codec[Schema] =
    (fields :: defaultField :: defaultOR)
      .as[(List[Field], String, Boolean)]
      .xmap(
        { case (fs, df, db) => new Schema(fs, df, db) },
        s => (s.fields, s.defaultField, s.defaultOR),
      )
}
