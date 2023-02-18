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

import cats.effect.IOApp
import cats.effect.IO
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import TokenStream.tokenizeSpaceV

object SearchApp extends IOApp.Simple {

  val index = CatIndex.index
  val indexLog =
    IO.println(s"Created index from ${CatIndex.docs.size} docs results in ${index.numTerms} terms")

  val bytes = Codec.termIndex.encode((index.numData, index.tfData), index.termDict)
  val bytesLog = IO.println(s"Encoded to ${bytes.toOption.get.size} bits")

  val decIndex = bytes.flatMap(Codec.termIndex.decodeValue)
  val decLog = decIndex match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val indexIO = IO.fromEither(decIndex.toEither.left.map(e => new Throwable(e.message)))
  def search(query: String) = {
    val q = tokenizeSpaceV(query)
    indexIO.flatMap(index => IO.println(q.map(index.docsWithTermTFIDF).mkString("\n")))
  }

  val query = "fast"

  val run = indexLog *> bytesLog *> decLog *> search(query) *> IO.println("- FIN -")

}
