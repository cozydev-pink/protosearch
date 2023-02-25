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
import scodec.bits.BitVector

object TermIndexCodec {
  val nl = BitVector.fromByte('\n')
  val str = codecs.utf8
  val strNl = codecs.vectorDelimited(nl, str)

  val vint = codecs.vint
  val termV = codecs.vectorOfN(vint, vint)
  val vecTermV = codecs.vectorOfN(vint, termV)

  val termIndex: Codec[TermIndexArray] =
    (vint :: vecTermV :: strNl)
      .as[(Int, Vector[Vector[Int]], Vector[String])]
      .xmap(
        TermIndexArray.unsafeFromTuple3,
        ti => (ti.numDocs, ti.tfData, ti.termDict),
      )
}

object MultiIndexCodec {
  import TermIndexCodec._

  val indexes: Codec[Map[String, TermIndexArray]] =
    codecs
      .listOfN(vint, (str :: termIndex).as[(String, TermIndexArray)])
      .xmap(_.toMap, _.toList)
  val defaultField: Codec[String] = str
  val defaultOr: Codec[Boolean] = codecs.bool
  val multiIndex: Codec[MultiIndex] =
    (indexes :: defaultField :: defaultOr)
      .as[(Map[String, TermIndexArray], String, Boolean)]
      .xmap(
        { case (in, dF, dOr) => MultiIndex.apply(in, dF, dOr) },
        mi => (mi.indexes, mi.defaultField, mi.defaultOR),
      )

}
