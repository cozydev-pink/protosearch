package pink.cozydev.protosearch.internal

import pink.cozydev.lucille.Query
import pink.cozydev.protosearch.PositionalIndex
import pink.cozydev.protosearch.analysis.Analyzer

class OrQueryIteratorSuite extends munit.FunSuite {

  val analyzer = Analyzer.default
  val index = PositionalIndex(
    List(
      analyzer.tokenize("0 1 2 3 4 5 6 7 8 9"),
      analyzer.tokenize("a b c d e f g h i j"),
      analyzer.tokenize("a 1 c 3 e 5 g 7 i 9"),
      analyzer.tokenize("0 b 2 d 4 f 6 h 8 j"),
    )
  )

  // Iters are stateful, so we make some constructors that return "thunks"
  case class TestQuery(label: String, iter: () => QueryIterator)
  object TestQuery {
    def noMatch: TestQuery = TestQuery("noMatch", () => new NoMatchQueryIterator)
    def exact(label: String, query: String): TestQuery =
      TestQuery(
        label,
        () => positionalIter(query),
      )

    private def positionalIter(q: String): PositionalIter =
      PositionalIter.exact(index, Query.Phrase(q)) match {
        case Some(iter) => iter
        case None =>
          val msg = s"some terms in query: '$q', could not be found in test index"
          throw new IllegalArgumentException(msg)
      }
  }

  // The order of QueryIterators should not matter, so we test all permutations
  def checkAllPermutations(
      queries: List[TestQuery],
      expected: List[Int],
      minShouldMatch: Int,
  )(implicit
      loc: munit.Location
  ): Unit =
    queries.permutations.foreach { tqs =>
      val name = tqs.map(_.label).mkString(", ")
      println(s"--- testing: $name")
      val iter = OrQueryIterator(tqs.map(tq => tq.iter()).toArray, minShouldMatch)
      val docs = iter.docs.toList
      println(s"--- result : $docs")
      assertEquals(docs, expected, clue = name)
    }

  test("always match with 1 matching, minShouldMatch=1 (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1), minShouldMatch = 1)
  }

  test("always match with 1 matching, minShouldMatch=1 (doc2)") {
    val queries = List(
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(2), minShouldMatch = 1)
  }

  test("always match with 1 matching, minShouldMatch=1 (last doc)") {
    assert(index.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(4), minShouldMatch = 1)
  }

  test("never match with 1 matching, minShouldMatch=2 (doc1)") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("never match with 1 matching, minShouldMatch=2 (doc2)") {
    val queries = List(
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("never match with 1 matching, minShouldMatch=2 (last doc)") {
    assert(index.numDocs == 4, clue = "expected 4 to be last docId")
    val queries = List(
      TestQuery.exact("doc4", "0 b 2 d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 2)
  }

  test("always match with 2 matching, minShouldMatch=1".only) {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1, 2), minShouldMatch = 1)
  }

  test("always match with 2 matching, minShouldMatch=2") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List(1, 2), minShouldMatch = 2)
  }

  test("never match with 2 matching, minShouldMatch=3") {
    val queries = List(
      TestQuery.exact("doc1", "0 1 2 3"),
      TestQuery.exact("doc2", "a b c d"),
      TestQuery.noMatch,
      TestQuery.noMatch,
    )
    checkAllPermutations(queries, List.empty, minShouldMatch = 3)
  }
}
