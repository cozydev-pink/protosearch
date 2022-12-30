package io.pig.protosearch

class InvertedIndexSuite extends munit.FunSuite {
  def tokenize(s: String): List[String] =
    s.split(" ").toList

  val docs = List(
    tokenize("the quick brown fox jumped over the lazy cat"),
    tokenize("the very fast cat jumped across the room"),
    tokenize("a lazy cat sleeps all day"),
  )

  lazy val index = InvertedIndex(docs)

  test("apply builds from list of lists of strings") {
    assertEquals(index.numTerms, 16)
  }

  test("docCount returns zero when no docs contain term") {
    assertEquals(index.docCount("???"), 0)
  }

  test("docCount returns number of documents containing term") {
    assertEquals(index.docCount("cat"), 3)
    assertEquals(index.docCount("lazy"), 2)
    assertEquals(index.docCount("room"), 1)
  }

  test("docsWithTerm returns empty list when no docs contain term") {
    assertEquals(index.docsWithTerm("???"), Nil)
  }

  test("docsWithTerm returns list of docIDs containing term") {
    assertEquals(index.docsWithTerm("cat").sorted, List(0, 1, 2))
    assertEquals(index.docsWithTerm("the").sorted, List(0, 1))
    assertEquals(index.docsWithTerm("lazy").sorted, List(0, 2))
  }

}
