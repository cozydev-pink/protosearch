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

import cats.data.NonEmptyList
import pink.cozydev.lucille.Query

case class IndexWithFields[A](index: MultiIndex, storedFields: List[A]) {

  def search(q: NonEmptyList[Query]): Either[String, List[A]] =
    index.search(q).map(hs => hs.map(storedFields(_)))

}
object IndexWithFields {
  import scodec.Codec
  import scodec.codecs._

  def codec[A](implicit codecA: Codec[A]): Codec[IndexWithFields[A]] =
    (
      ("MultiIndex" | MultiIndex.codec) ::
        ("list of storedField" | listOfN(vint, variableSizeBytes(vint, codecA)))
    )
      .as[(MultiIndex, List[A])]
      .xmap(
        { case (i, as) => IndexWithFields(i, as) },
        { case (b: IndexWithFields[A]) => (b.index, b.storedFields) },
      )

  val strCodec: Codec[String] = variableSizeBytes(vint, utf8)
}
