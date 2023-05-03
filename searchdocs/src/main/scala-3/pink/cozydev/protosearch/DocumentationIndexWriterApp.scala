package pink.cozydev.protosearch

import cats.syntax.all._
import cats.effect.{IO, IOApp}
import cats.data.NonEmptyList
import io.circe.Json
import io.circe.syntax._
import fs2.{Chunk, Stream}
import fs2.io.file.{Files, Path}
import fs2.text.utf8
import pink.cozydev.protosearch.analysis.IngestMarkdown
import laika.parse.markup.DocumentParser.RendererError
import pink.cozydev.protosearch.analysis.SubDocument

object DocumentationIndexWriterApp extends IOApp.Simple {

  import DocumentationSearch._

  val pathHttp4s = Path("/home/andrew/grabbed/http4s")
  val pathHttp4sDocs = pathHttp4s / "docs/docs/"

  def readAsString(p: Path): IO[String] =
    Files[IO].readAll(p).through(fs2.text.utf8.decode).compile.string

  def collectDocs(
      subDocs: Either[RendererError, NonEmptyList[SubDocument]],
      path: Path,
  ): List[Doc] =
    subDocs match {
      case Left(_) => Nil // swallow errors
      case Right(docs) =>
        docs.toList.map(sd => Doc(path.fileName.toString, sd.title, sd.anchor, sd.content))
    }

  def docsFromPath(pathToDocs: Path): IO[List[Doc]] =
    Files[IO]
      .walk(pathToDocs)
      .filter(_.extName == ".md")
      .evalMap(p => readAsString(p).product(IO.pure(p)))
      .map { case (content, p) => (IngestMarkdown.transformUnresolved(content), p) }
      .flatMap(ds => Stream.emits(collectDocs.tupled(ds)))
      .compile
      .toList

  val docsAndIndex: IO[(List[Doc], MultiIndex)] = docsFromPath(pathHttp4sDocs)
    .map(docs => (docs, searchSchema.indexBldr("body")(docs)))

  val indexPath = Path("./searchdocs-web/searchdocs/http4s-docs.idx")
  val indexWriter = Files[IO].writeAll(indexPath)

  val docsPath = Path("./searchdocs-web/searchdocs/http4s-docs.json")
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
