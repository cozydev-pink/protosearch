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

import cats.effect.IO
import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}
import fs2.Stream
import pink.cozydev.protosearch.analysis.Analyzer
import scodec.bits.ByteVector

case class Doc(fileName: String, title: String, anchor: Option[String], body: String)
object Doc {
  implicit val docEncoder: Encoder[Doc] = deriveEncoder
  implicit val docDecoder: Decoder[Doc] = deriveDecoder
}
case class Hit(doc: Doc, score: Double)

object DocumentationSearch {

  val analyzer = Analyzer.default.withLowerCasing
  val searchSchema = SearchSchema[Doc](
    ("fileName", _.fileName, analyzer),
    ("title", _.title, analyzer),
    ("body", _.body, analyzer),
  )

  def parseIndexBytes(s: Stream[IO, Byte]): IO[MultiIndex] =
    s.compile.to(ByteVector).map(bv => MultiIndex.codec.decodeValue(bv.bits).require)

  def searchBldr(docs: List[Doc]): String => Either[String, List[Hit]] = {
    val index = searchSchema.indexBldr("body")(docs)
    val qAnalyzer = searchSchema.queryAnalyzer("body")
    val scorer = Scorer(index)
    qs =>
      qAnalyzer
        .parse(qs)
        .flatMap(q => index.search(q.qs).flatMap(ds => scorer.score(q.qs, ds.toSet)))
        .map(hits =>
          hits
            .map((i, score) => Hit(docs(i), score))
            .sortBy(h => -h.score)
        )
  }

}
