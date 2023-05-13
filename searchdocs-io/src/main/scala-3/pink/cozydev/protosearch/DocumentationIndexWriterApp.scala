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

import cats.syntax.all._
import cats.effect.{IO, IOApp}
import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax._
import fs2.{Chunk, Stream}
import fs2.io.file.{Files, Path}
import fs2.text.utf8
import laika.parse.markup.DocumentParser.RendererError
import pink.cozydev.protosearch.analysis.IngestMarkdown
import pink.cozydev.protosearch.analysis.DocsDirectory
import pink.cozydev.protosearch.analysis.SubDocument

object DocumentationIndexWriterApp extends IOApp.Simple {

  import DocumentationSearch._

  val pathHttp4s = Path("/home/andrew/grabbed/http4s")
  val pathHttp4sDocs = pathHttp4s / "docs/docs/"

  def readAsString(p: Path): IO[String] =
    Files[IO].readAll(p).through(fs2.text.utf8.decode).compile.string

  def collectDocs(
      subDocs: Either[RendererError, NonEmptyList[SubDocument]]
  ): List[Doc] =
    subDocs match {
      case Left(_) => Nil // swallow errors
      case Right(docs) =>
        docs.toList.map(sd => Doc(sd.fileName, sd.title, sd.anchor, sd.content))
    }

  def docsFromPath(pathToDocs: Path): IO[List[Doc]] =
    Files[IO]
      .walk(pathToDocs)
      .filter(_.extName == ".md")
      .evalMap(p => readAsString(p))
      .map(IngestMarkdown.transformUnresolved)
      .flatMap(ds => Stream.emits(collectDocs(ds)))
      .compile
      .toList

  def docsFromPathNew(pathToDocs: Path): IO[List[Doc]] =
    DocsDirectory
      .dirToDocs(pathToDocs.toString)
      .map(subDocBundles => subDocBundles.toList.flatMap(collectDocs))

  val docsAndIndex: IO[(List[Doc], MultiIndex)] = docsFromPathNew(pathHttp4sDocs)
    .map(docs => (docs, searchSchema.indexBldr("body")(docs)))

  val indexPath = Path("./docs/searchdocs/http4s-docs.idx")
  val indexWriter = Files[IO].writeAll(indexPath)

  val docsPath = Path("./docs/searchdocs/http4s-docs.json")
  val docsWriter = Files[IO].writeAll(docsPath)

  def writeIndex(idx: MultiIndex): IO[Unit] = {
    val bytes = MultiIndex.codec.encode(idx).require.bytes
    val stream = Stream.chunk(Chunk.byteVector(bytes))
    stream.through(indexWriter).compile.drain
  }

  def writeJson(docs: List[Doc]): IO[Unit] = {
    val bytes = Stream.emit(docs.asJson.noSpaces).through(utf8.encode)
    bytes.through(docsWriter).compile.drain
  }

  val run =
    docsAndIndex.flatMap((docs, index) => (writeJson(docs), writeIndex(index)).parTupled).void
}
