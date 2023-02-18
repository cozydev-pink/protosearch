package io.pig.protosearch

import TokenStream.tokenizeSpaceV

object CatIndex {
  val docs: Vector[Vector[String]] =
    Vector(
      tokenizeSpaceV("the quick brown fox jumped over the lazy cat"),
      tokenizeSpaceV("the very fast cat jumped across the room"),
      tokenizeSpaceV("a lazy cat sleeps all day"),
    )

  lazy val index = TermIndexArray(docs)
}
