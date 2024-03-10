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

import scala.scalajs.js.annotation._
import scala.scalajs.js
import org.scalajs.dom.Blob
import scodec.bits.ByteVector

@JSExportTopLevel("Hit")
class JsHit(
    val id: Int,
    val score: Double,
    val fields: js.Dictionary[String],
) extends js.Object

@JSExportTopLevel("Querier")
class Querier(val mIndex: MultiIndex, val defaultField: String) {
  import js.JSConverters._

  private val scorer = Scorer(mIndex)
  private val qAnalyzer = mIndex.queryAnalyzer

  @JSExport
  def search(query: String): js.Array[JsHit] = {
    val hits = mIndex
      .searchInteractive(query)
      .toOption
      .getOrElse(Nil)
      .map(h => new JsHit(h.id, h.score, h.fields.toJSDictionary))
    hits.toJSArray
  }
}

@JSExportTopLevel("QuerierBuilder")
object QuerierBuilder {
  private def decode(buf: js.typedarray.ArrayBuffer): MultiIndex = {
    val bv = ByteVector.view(buf)
    MultiIndex.codec.decodeValue(bv.bits).require
  }

  // TODO the default field is in the MultiIndex, just use that
  @JSExport
  def load(bytes: Blob, defaultField: String): js.Promise[Querier] =
    bytes.arrayBuffer().`then`[Querier] { buf =>
      val mIndex = decode(buf)
      new Querier(mIndex, defaultField)
    }

}
