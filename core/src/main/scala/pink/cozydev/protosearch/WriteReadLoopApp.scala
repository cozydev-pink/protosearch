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
import fs2.Stream
import fs2.io.file.{Files, Path}
import scodec.Attempt.Failure
import scodec.Attempt.Successful
import fs2.Chunk
import scodec.bits.ByteVector

object WriteIndexApp extends IOApp.Simple {
  import BookIndex.{Book, corpus}

  val analyzer = Analyzer.default.withLowerCasing

  val index = MultiIndex.apply[Book](
    "title",
    ("title", _.title, analyzer),
    ("author", _.author, analyzer),
  )(corpus)

  val indexBytes = MultiIndex.codec.encode(index).map(_.bytes)

  val file = Path("book-index.dat")
  val writer = Files[IO].writeAll(file)

  val run = (indexBytes match {
    case Failure(err) => Stream.eval(IO.println(s"Error encoding index: $err"))
    case Successful(bytes) => Stream.chunk(Chunk.byteVector(bytes)).through(writer)
  }).compile.drain

}

object ReadIndexApp extends IOApp.Simple {
  import BookIndex.{Book, corpus}

  val file = Path("book-index.dat")
  val reader: Stream[IO, Byte] = Files[IO].readAll(file)

  val analyzer = Analyzer.default.withLowerCasing
  val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

  def search(index: MultiIndex)(qs: String): Either[String, List[Book]] = {
    val q = qAnalyzer.parse(qs)
    println(s"+++ analyzed query: $q")
    val result = q.flatMap(index.search)
    // TODO vector index access is unsafe
    result.map(hits => hits.map(i => corpus(i)))
  }

  val indexIO = reader.compile
    .to(ByteVector)
    .map(bv => MultiIndex.codec.decodeValue(bv.bits))
    .map(_.toTry)
    .flatMap(IO.fromTry)

  val run =
    indexIO.flatMap { index =>
      search(index)("Two AND author:suess") match {
        case Left(err) => IO.println(s"Error while searching: $err")
        case Right(hits) => IO.println(s"Found: $hits")
      }
    }
}
