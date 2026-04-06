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

import pink.cozydev.protosearch.analysis.{Analyzer, QueryAnalyzer}
import fixtures.BookIndex

class SearcherSuite extends munit.FunSuite {
  import BookIndex.*

  val analyzer = Analyzer.default.withLowerCasing
  val index = IndexBuilder
    .of[Book](
      (Field("title", analyzer, true, true, true), _.title),
      (Field("author", analyzer, true, true, false), _.author)
    )
    .fromList(allBooks)

  val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

  val searcher = Searcher.default(index)

  def search(q: String): Either[String, Set[Book]] = {
    val req = SearchRequest.default(q)
    val result = searcher.search(req)
    result.toEither.map(hits => hits.map(h => allBooks(h.id - 1)).toSet)
  }

  def searchHit(q: String): Either[String, List[(Int, Map[String, String])]] = {
    val req = SearchRequest(q, 10, 0, None, Some(List("title", "author")), false)
    val results = searcher.search(req)
    results.toEither.map(_.map(h => (h.id, h.fields)))
  }

  test("Term searchHit fields") {
    val books = searchHit("Bad")
    val miceMap = Map(
      "title" -> "The Tale of Two Bad Mice",
      "author" -> "Beatrix Potter"
    )
    assertEquals(books, Right(List(2 -> miceMap)))
  }

  test("Term") {
    val books = search("Bad")
    assertEquals(books, Right(Set(mice)))
  }

  test("Term lowercased") {
    val books = search("bad")
    assertEquals(books, Right(Set(mice)))
  }

  test("AND with fieldQ") {
    val books = search("two AND author:Seuss")
    assertEquals(books, Right(Set(fish)))
  }

  test("AND with multiple fieldQ") {
    val books = search("title:two AND author:Seuss")
    assertEquals(books, Right(Set(fish)))
  }

  test("AND NOT field TermRange") {
    val books = search("TWO AND NOT author:[r TO t]")
    assertEquals(books, Right(Set(mice)))
  }

  test("AND with field TermRange") {
    val books = search("TWO AND author:[a TO e]")
    assertEquals(books, Right(Set(mice, fish)))
  }

  test("implicit OR with field TermRange") {
    val books = search("two author:[a TO c]")
    assertEquals(books, Right(Set(peter, mice, fish)))
  }

  test("explicit OR") {
    val books = search("tale OR two")
    assertEquals(books, Right(Set(peter, mice, fish)))
  }

  test("explicit OR with field TermRange") {
    val books = search("two OR author:[a TO c]")
    assertEquals(books, Right(Set(peter, mice, fish)))
  }

  test("field Group") {
    val books = search("author:(potter seuss)")
    assertEquals(books, Right(Set(peter, mice, fish, eggs)))
  }

  test("field Group with NOT") {
    val books = search("author:(potter seuss) AND NOT eggs")
    assertEquals(books, Right(Set(peter, mice, fish)))
  }

  test("nested Groups") {
    val books = search("(eggs AND ham AND author:(potter SEUSS))")
    assertEquals(books, Right(Set(eggs)))
  }

  test("single term phrase query (original casing)") {
    val books = search("\"Eggs\"")
    assertEquals(books, Right(Set(eggs)))
  }

  test("single term phrase query") {
    val books = search("\"eggs\"")
    assertEquals(books, Right(Set(eggs)))
  }

  test("multi term phrase query") {
    val books = search("\"Green Eggs\"")
    assertEquals(books, Right(Set(eggs)))
  }

  test("regex") {
    val books = search("/e(r|e)/")
    assertEquals(books, Right(Set(peter, eggs)))
  }

  test("regex fail") {
    val q = search("/[a/")
    val err = "Invalid regex query TermRegex([a)"
    assertEquals(
      q,
      Left(err)
    )
  }

  test("error when highlight field not stored") {
    val req = SearchRequest("bad", 10, 0, Some(List("fakefield")), None, false)
    val result = searcher.search(req)
    val err = "Highlights not stored in index: 'fakefield'."
    assertEquals(result, SearchFailure(err))
  }

  test("error when result field not stored") {
    val req = SearchRequest("bad", 10, 0, None, Some(List("fakefield")), false)
    val result = searcher.search(req)
    val err = "Fields not stored in index: 'fakefield'."
    assertEquals(result, SearchFailure(err))
  }

  test("error when highlight and result fields not stored") {
    val req = SearchRequest("bad", 10, 0, Some(List("fakefield")), Some(List("fakefield")), false)
    val result = searcher.search(req)
    val err =
      "Fields not stored in index: 'fakefield'. Highlights not stored in index: 'fakefield'."
    assertEquals(result, SearchFailure(err))
  }

  test("error when highlight and result fields not stored") {
    val req = SearchRequest(
      "bad",
      10,
      0,
      Some(List("fakefield")),
      Some(List("fakefield1", "fakefield2")),
      false
    )
    val result = searcher.search(req)
    val err =
      "Fields not stored in index: 'fakefield1, fakefield2'. Highlights not stored in index: 'fakefield'."
    assertEquals(result, SearchFailure(err))
  }

  test("no highlights when highlightFields is Some(Nil)") {
    val req = SearchRequest("bad", 10, 0, Some(Nil), Some(List("title", "author")), false)
    val result = searcher.search(req)
    val hits = result.toEither.toOption.get
    assert(hits.nonEmpty, "expected at least one hit")
    hits.foreach(h => assertEquals(h.highlights, Map.empty[String, String]))
  }

  test("SearchRequest.size limits the number of results") {
    val matchesAll = "author:(potter seuss)"
    for (i <- 0 to 4) {
      val req = SearchRequest(matchesAll, i, 0, None, None, false)
      val numResults = searcher.search(req).fold(_ => 0, _.size)
      assertEquals(numResults, i)
    }
  }

  test("SearchRequest.size greater than number of results works fine") {
    val matchesAll = "author:(potter seuss)"
    val req = SearchRequest(matchesAll, 1000, 0, None, None, false)
    val numResults = searcher.search(req).fold(_ => 0, _.size)
    assertEquals(numResults, 4)
  }

  test("SearchRequest.skip skips the top N results") {
    val matchesAll = "author:(potter seuss)"
    // skip first three, so only one result remains
    val req = SearchRequest(matchesAll, 1000, 3, None, None, false)
    val numResults = searcher.search(req).fold(_ => 0, _.size)
    assertEquals(numResults, 1)
  }

}
