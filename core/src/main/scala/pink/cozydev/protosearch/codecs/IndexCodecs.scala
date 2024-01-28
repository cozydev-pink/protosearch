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

package pink.cozydev.protosearch.codecs

import scodec.{Attempt, Codec, Err, codecs}
import scala.reflect.ClassTag

// Derived from scodec vectorOfN
// https://github.com/scodec/scodec/blob/v2.2.1/shared/src/main/scala/scodec/codecs/codecs.scala#L1106
object IndexCodecs {
  def arrayOfN[A: ClassTag](countCodec: Codec[Int], valueCodec: Codec[A]): Codec[Array[A]] =
    countCodec
      .flatZip(count => new ArrayCodec(valueCodec, Some(count)))
      .narrow(
        { case (cnt, xs) =>
          if (xs.size == cnt) Attempt.successful(xs)
          else {
            val valueBits = valueCodec.sizeBound.exact.getOrElse(valueCodec.sizeBound.lowerBound)
            Attempt.failure(Err.insufficientBits(cnt * valueBits, xs.size * valueBits))
          }
        },
        (xs: Array[A]) => (xs.size, xs),
      )
      .withToString(s"arrayOfN($countCodec, $valueCodec)")

  val termList: Codec[Array[String]] =
    arrayOfN(codecs.vint, codecs.utf8_32).withContext("termList")
}
