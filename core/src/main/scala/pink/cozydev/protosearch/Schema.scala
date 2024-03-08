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

/* A Schema describes the fields of a document */
final class Schema private (
    private val fields: List[Field]
) {
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
      tail: Field*
  ): Schema =
    new Schema(head :: tail.toList)

  val codec: Codec[Schema] =
    codecs
      .listOfN(codecs.vint, Field.codec)
      .xmap(
        fs => new Schema(fs),
        s => s.fields,
      )
}
