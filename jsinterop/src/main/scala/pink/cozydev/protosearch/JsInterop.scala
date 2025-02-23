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

import pink.cozydev.protosearch.highlight.{FirstMatchHighlighter, FragmentFormatter}
import scala.scalajs.js.annotation._
import scala.scalajs.js
import org.scalajs.dom.Blob
import scodec.bits.ByteVector

@JSExportTopLevel("Hit")
class JsHit(
    val id: Int,
    val score: Double,
    val fields: js.Dictionary[String],
    val highlights: js.Dictionary[String],
) extends js.Object

@JSExportTopLevel("Querier")
class Querier(val mIndex: MultiIndex) {
  import js.JSConverters._
  val highlighter =
    FirstMatchHighlighter(FragmentFormatter(150, "<mark>", "</mark>"))
  val searcher = SearchInterpreter(mIndex, highlighter)
  val highlightFields = List("title", "body")
  val resultFields = List("body", "path", "title")

  @JSExport
  def search(query: String): js.Array[JsHit] = {
    val req = SearchRequest(query, size = 10, highlightFields, resultFields, lastTermPrefix = true)
    val hits = searcher
      .search(req)
      .fold(
        err => { println(err); Nil },
        identity,
      )
      .map(h => new JsHit(h.id, h.score, h.fields.toJSDictionary, h.highlights.toJSDictionary))
    hits.toJSArray
  }
}

@JSExportTopLevel("QuerierBuilder")
object QuerierBuilder {
  private def decode(buf: js.typedarray.ArrayBuffer): MultiIndex = {
    val bv = ByteVector.view(buf)
    MultiIndex.codec.decodeValue(bv.bits).require
  }

  @JSExport
  def load(bytes: Blob): js.Promise[Querier] =
    bytes.arrayBuffer().`then`[Querier] { buf =>
      val mIndex = decode(buf)
      new Querier(mIndex)
    }

}
