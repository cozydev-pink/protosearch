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

import pink.cozydev.protosearch.analysis.Analyzer

/** A part of a Document.
  *
  * @param name Field name as would be used in a field query, e.g. `title:cats`
  * @param analyzer The analyzer to apply to the field at indexing time
  * @param stored Whether the raw field value should be stored in the index
  * @param indexed Whether the field value should be indexed for fast querying
  * @param positions Whether the positions should be indexed for fast phrase querying
  */
case class Field(
    name: String,
    analyzer: Analyzer,
    stored: Boolean,
    indexed: Boolean,
    positions: Boolean,
)
object Field {
  import scodec.Codec
  import scodec.codecs._

  val codec: Codec[Field] = {
    ("name" | utf8_32) ::
      ("analyzer" | Analyzer.codec) ::
      ("stored" | bool) ::
      ("indexed" | bool) ::
      ("positions" | bool)
  }.as[Field]
}
