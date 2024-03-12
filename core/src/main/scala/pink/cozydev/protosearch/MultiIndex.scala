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

import pink.cozydev.lucille.Query
import cats.data.NonEmptyList

case class MultiIndex(
    indexes: Map[String, Index],
    schema: Schema,
    fields: Map[String, Array[String]],
) {

  private val indexSearcher = IndexSearcher(this, schema.defaultOR)
  private val scorer = Scorer(this, schema.defaultOR)
  val queryAnalyzer = schema.queryAnalyzer(schema.defaultField)

  /** Search the index with a `Query`. Results are sorted by descending score.
    *
    * @param q The `Query` to search
    * @return A list of `Hit`s or error
    */
  def search(q: Query): Either[String, List[Hit]] = {
    val docs = indexSearcher.search(q).flatMap(ds => scorer.score(NonEmptyList.one(q), ds))
    val lstb = List.newBuilder[Hit]
    docs.map(_.foreach { case (docId, score) =>
      lstb += Hit(docId, score, fields.map { case (k, v) => (k, v(docId)) })
    })
    docs.map(_ => lstb.result())
  }

  /** Search the index with a Lucene syntax string. Results are sorted by descending score.
    *
    * @param q The query string to search
    * @return A list of `Hit`s or error
    */
  def search(q: String): Either[String, List[Hit]] =
    queryAnalyzer.parse(q).flatMap(search)

  /** Search the index with a possibly incomplete query. Meant for use in a "search as your type"
    * scenario. The last term, which is possibly incomplete, is rewritten to be a prefix.
    *
    * @param partialQuery The possibly incomplete lucene query string
    * @return A list of `Hit`s or error
    */
  def searchInteractive(partialQuery: String): Either[String, List[Hit]] = {
    val rewriteQ =
      queryAnalyzer.parse(partialQuery).map(mq => mq.mapLastTerm(LastTermRewrite.termToPrefix))
    rewriteQ.flatMap(search)
  }
}
object MultiIndex {
  import pink.cozydev.protosearch.codecs.IndexCodecs

  import scodec.{Codec, codecs}
  import scodec.codecs._
  import scodec.{Attempt, Err}

  def encodeIndex(i: Index): Attempt[Either[FrequencyIndex, PositionalIndex]] =
    i match {
      case idx: PositionalIndex => Attempt.successful(Right(idx))
      case idx: FrequencyIndex => Attempt.successful(Left(idx))
      case _ => Attempt.failure(Err("Index was neither FrequencyIndex or PositionalIndex"))
    }

  val codec: Codec[MultiIndex] = {

    val index: Codec[Index] = discriminated[Either[FrequencyIndex, PositionalIndex]]
      .by(uint2)
      .caseP(0) { case Left(l) => l }(Left.apply)(FrequencyIndex.codec)
      .caseP(1) { case Right(r) => r }(Right.apply)(PositionalIndex.codec)
      .widen(eitherFP => eitherFP.merge, encodeIndex)

    val indexes: Codec[Map[String, Index]] =
      codecs
        .listOfN(codecs.vint, (codecs.utf8_32 :: index).as[(String, Index)])
        .xmap(_.toMap, _.toList)

    val fieldStrings: Codec[Array[String]] =
      IndexCodecs.arrayOfN(codecs.vint, codecs.variableSizeBytes(codecs.vint, codecs.utf8))

    val fields: Codec[Map[String, Array[String]]] =
      codecs
        .listOfN(codecs.vint, (codecs.utf8_32 :: fieldStrings).as[(String, Array[String])])
        .xmap(_.toMap, _.toList)

    val multiIndex: Codec[MultiIndex] =
      (indexes :: Schema.codec :: fields)
        .as[(Map[String, Index], Schema, Map[String, Array[String]])]
        .xmap(
          { case (in, sc, fs) => MultiIndex.apply(in, sc, fs) },
          mi => (mi.indexes, mi.schema, mi.fields),
        )
    multiIndex
  }
}
