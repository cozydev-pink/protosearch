package io.pig.protosearch

import fs2.{Chunk, Pure, Stream}

object TokenStream {
  private def splitSpace(s: String): Chunk[String] =
    Chunk.array(s.split(" "))

  def tokenizeSpace(s: String): Stream[Pure, String] =
    Stream.chunk(splitSpace(s))

  def tokenizeSpaceV(s: String): Vector[String] =
    s.split(" ").toVector
}
