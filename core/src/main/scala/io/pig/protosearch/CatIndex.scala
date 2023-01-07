package io.pig.protosearch

object CatIndex {
  def tokenize(s: String): List[String] =
    s.split(" ").toList

  val docs = List(
    tokenize("the quick brown fox jumped over the lazy cat"),
    tokenize("the very fast cat jumped across the room"),
    tokenize("a lazy cat sleeps all day"),
  )

  lazy val index = TermIndexArray(docs)
}
