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

import scodec._
import scodec.bits.BitVector
import scala.collection.immutable.ArraySeq
import scala.reflect.ClassTag

// Derived from scodec VectorCodec
// https://github.com/scodec/scodec/blob/series/1.11.x/shared/src/main/scala/scodec/codecs/VectorCodec.scala
private[protosearch] final class ArrayCodec[A: ClassTag](codec: Codec[A], limit: Option[Int] = None)
    extends Codec[Array[A]] {

  def sizeBound: SizeBound = limit match {
    case None => SizeBound.unknown
    case Some(lim) => codec.sizeBound * lim.toLong
  }

  def encode(array: Array[A]) =
    // safe, encodeSeq does not mutate the array
    Encoder.encodeSeq(codec)(ArraySeq.unsafeWrapArray(array))

  def decode(buffer: BitVector) =
    Decoder.decodeCollect[Array, A](codec, limit)(buffer)

  override def toString = s"arrayCodec($codec)"
}
