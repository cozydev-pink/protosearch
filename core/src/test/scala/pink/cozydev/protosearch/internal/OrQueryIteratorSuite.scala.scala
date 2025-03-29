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

  def exact(phrase: String): PositionalIter =
    PositionalIter.exact(index, Query.Phrase(phrase)).get

  val matchNone = NoMatchQueryIterator

  test("always match with 1 matching, minShouldMatch=1") {
    val queries = Map(
      "matchAB" -> exact("0 1 2 3"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 1)
      val docs = iter.docs.toList
      assertEquals(docs, List(1), clue = name)
    }
  }

  test("always match with 1 matching, minShouldMatch=1") {
    val queries = Map(
      "matchAB" -> exact("a b c d"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 1)
      val docs = iter.docs.toList
      assertEquals(docs, List(2), clue = name)
    }
  }

  test("never match with 1 matching, minShouldMatch=2") {
    val queries = Map(
      "matchAB" -> exact("a b c d"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 2)
      val docs = iter.docs.toList
      assertEquals(docs, List.empty, clue = name)
    }
  }

  test("always match with 2 matching, minShouldMatch=1") {
    val queries = Map(
      "matchAB" -> exact("a b c d"),
      "match01" -> exact("0 1 2 3"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 1)
      val docs = iter.docs.toList
      assertEquals(docs, List(1, 2), clue = name)
    }
  }

  test("always match with 2 matching, minShouldMatch=2") {
    val queries = Map(
      "matchAB" -> exact("a b c d"),
      "match01" -> exact("0 1 2 3"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 2)
      val docs = iter.docs.toList
      assertEquals(docs, List(1, 2), clue = name)
    }
  }

  test("never match with 2 matching, minShouldMatch=3") {
    val queries = Map(
      "matchAB" -> exact("a b c d"),
      "match01" -> exact("0 1 2 3"),
      "noMatch1" -> matchNone,
      "noMatch2" -> matchNone,
    )
    queries.toList.permutations.foreach { label_qis =>
      val (labels, qis) = label_qis.unzip
      val name = labels.mkString(", ")
      val iter = OrQueryIterator(qis.toArray, 3)
      val docs = iter.docs.toList
      assertEquals(docs, List.empty, clue = name)
    }
  }

//   test("do it") {
//     val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
//     val iter2 = PositionalIter.exact(index, Query.Phrase("jumped")).get
//     val iter3 = PositionalIter.exact(index, Query.Phrase("cat")).get
//     val iter4 = PositionalIter.exact(index, Query.Phrase("the room")).get
//     val iter = new OrQueryIterator(Array(iter1, iter2, iter3, iter4), 1)
//     val docs = iter.takeWhile(_ > -1).toSet
//     assertEquals(docs, Set(1, 2))
//   }

//   test("do it") {
//     val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
//     val iter2 = PositionalIter.exact(index, Query.Phrase("jumped")).get
//     val iter3 = PositionalIter.exact(index, Query.Phrase("cat")).get
//     val iter4 = PositionalIter.exact(index, Query.Phrase("the room")).get
//     val iter = new OrQueryIterator(Array(iter1, iter2, iter3, iter4), 3)
//     val docs = iter.takeWhile(_ > -1).toSet
//     assertEquals(docs, Set(1))
//   }

//   test("do it") {
//     val iter1 = PositionalIter.exact(index, Query.Phrase("very fast cat")).get
//     val iter2 = PositionalIter.exact(index, Query.Phrase("day")).get
//     val iter = new OrQueryIterator(Array(iter1, iter2), 2)
//     val docs = iter.takeWhile(_ > -1).toSet
//     assert(docs.isEmpty)
//   }
}
