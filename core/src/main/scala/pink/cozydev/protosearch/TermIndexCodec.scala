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

import scodec._

object TermIndexCodec {
  val vint = codecs.vint

  val terms = IndexCodecs.termList

  val termV = codecs.vectorOfN(vint, vint)
  val vecTermV = codecs.vectorOfN(vint, termV).withContext("term frequencies")

  val numDocs = vint.withContext("numDocs")
  val termIndex: Codec[TermIndexArray] =
    (numDocs :: vecTermV :: terms)
      .as[(Int, Vector[Vector[Int]], Array[String])]
      .xmap(
        TermIndexArray.unsafeFromTuple3,
        ti => ti.serializeToTuple3,
      )
}

object MultiIndexCodec {
  import TermIndexCodec._

  val indexes: Codec[Map[String, TermIndexArray]] =
    codecs
      .listOfN(vint, (codecs.utf8_32 :: termIndex).as[(String, TermIndexArray)])
      .xmap(_.toMap, _.toList)
  val defaultField: Codec[String] = codecs.utf8_32.withContext("defaultField")
  val defaultOr: Codec[Boolean] = codecs.bool.withContext("defaultOr")
  val multiIndex: Codec[MultiIndex] =
    (indexes :: defaultField :: defaultOr)
      .as[(Map[String, TermIndexArray], String, Boolean)]
      .xmap(
        { case (in, dF, dOr) => MultiIndex.apply(in, dF, dOr) },
        mi => (mi.indexes, mi.defaultField, mi.defaultOR),
      )

}
