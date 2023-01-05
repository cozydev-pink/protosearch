package io.pig.protosearch

import cats.effect.IOApp
import cats.effect.IO
import scodec.Attempt.Failure
import scodec.Attempt.Successful

object SearchApp extends IOApp.Simple {

  def tokenize(s: String): List[String] =
    s.split(" ").toList

  val docs = List(
    tokenize("the quick brown fox jumped over the lazy cat"),
    tokenize("the very fast cat jumped across the room"),
    tokenize("a lazy cat sleeps all day"),
  )

  val index = TermIndexArray(docs)
  val indexLog =
    IO.println(s"Created index from ${docs.size} docs results in ${index.numTerms} terms")

  val bytes = Codec.termIndex.encode(index.tfData, index.termDict)
  val bytesLog = IO.println(s"Encoded to ${bytes.toOption.get.size} bytes")

  val decIndex = bytes.flatMap(Codec.termIndex.decodeValue)
  val decLog = decIndex match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val indexIO = IO.fromEither(decIndex.toEither.left.map(e => new Throwable(e.message)))
  def search(query: String) = {
    val q = tokenize(query)
    indexIO.flatMap(index => IO.println(q.map(index.docsWithTermTFIDF).mkString("\n")))
  }

  val query = "fast"

  val run = indexLog *> bytesLog *> decLog *> search(query) *> IO.println("- FIN -")

}
