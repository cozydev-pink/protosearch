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
import cats.effect.IOApp
import cats.effect.IO
import scodec.Attempt.Failure
import scodec.Attempt.Successful

object Codec {
  val nl = BitVector.fromByte('\n')
  val str = codecs.utf8
  val strNl = codecs.vectorDelimited(nl, str)

  val vint = codecs.vint
  val termV = codecs.vectorOfN(vint, vint)
  val vecTermV = codecs.vectorOfN(vint, termV)

  val termIndex = (vint ~ vecTermV ~ strNl).map { case ((numDocs, vec), terms) =>
    TermIndexArray.unsafeFromVecs(vec, terms, numDocs)
  }
}
object CodecApp extends IOApp.Simple {

  val xs = Vector("hello", "world")
  val enc = Codec.strNl.encode(xs)
  val dec = enc.flatMap(Codec.strNl.decodeValue)
  val prog = dec match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val termVectors = Vector(
    Vector(1, 2, 3, 4),
    Vector(1, 2, 3, 4),
    Vector(1, 2, 3, 4),
  )
  val decV = Codec.vecTermV.encode(termVectors).flatMap(Codec.vecTermV.decodeValue)
  val decProg = decV match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val run = prog *> decProg *> IO.println("- FIN -")
}
