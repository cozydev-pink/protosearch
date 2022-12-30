package io.pig.protosearch

class InvertedIndexSuite extends munit.FunSuite {
  def tokenize(s: String): List[String] =
    s.split(" ").toList

  val docs = List(
    tokenize("the quick brown fox jumped over the lazy cat"),
    tokenize("the very fast cat jumped across the room"),
    tokenize("a lazy cat sleeps all day"),
  )

  test("InvertedIndex.apply builds from list of lists of strings") {
    assertEquals(InvertedIndex(docs).numTerms, 16)
  }

  test("InvertedIndex docCount returns zero when no docs contain term") {
    assertEquals(InvertedIndex(docs).docCount("???"), 0)
  }

  test("InvertedIndex docCount returns number of documents containing term") {
    assertEquals(InvertedIndex(docs).docCount("cat"), 3)
    assertEquals(InvertedIndex(docs).docCount("lazy"), 2)
    assertEquals(InvertedIndex(docs).docCount("room"), 1)
  }

  test("InvertedIndex docsWithTerm returns empty list when no docs contain term") {
    assertEquals(InvertedIndex(docs).docsWithTerm("???"), Nil)
  }

  test("InvertedIndex docsWithTerm returns list of docIDs containing term") {
    val index = InvertedIndex(docs)
    assertEquals(index.docsWithTerm("cat").sorted, List(0, 1, 2))
    assertEquals(index.docsWithTerm("the").sorted, List(0, 1))
    assertEquals(index.docsWithTerm("lazy").sorted, List(0, 2))
  }

}
