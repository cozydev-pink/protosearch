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

class MultiIndexSuite extends munit.FunSuite {
  import BookIndex._

  val analyzer = Analyzer.default.withLowerCasing
  val index = IndexBuilder
    .of[Book](
      (Field("title", analyzer, true, true, true), _.title),
      (Field("author", analyzer, true, true, false), _.author),
    )
    .fromList(allBooks)

  val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

  def search(q: String): Either[String, List[Book]] = {
    val result = index.search(q)
    result.map(hits => hits.map(h => allBooks(h.id)))
  }

  def searchHit(q: String): Either[String, List[(Int, Map[String, String])]] =
    index.search(q).map(_.map(h => (h.id, h.fields)))

  test("Term searchHit fields") {
    val books = searchHit("Bad")
    val miceMap = Map(
      "title" -> "The Tale of Two Bad Mice",
      "author" -> "Beatrix Potter",
    )
    assertEquals(books, Right(List(1 -> miceMap)))
  }

  test("Term") {
    val books = search("Bad")
    assertEquals(books, Right(List(mice)))
  }

  test("Term lowercased") {
    val books = search("bad")
    assertEquals(books, Right(List(mice)))
  }

  test("AND with fieldQ") {
    val books = search("two AND author:Seuss")
    assertEquals(books, Right(List(fish)))
  }

  test("AND with multiple fieldQ") {
    val books = search("title:two AND author:Seuss")
    assertEquals(books, Right(List(fish)))
  }

  test("AND NOT field TermRange") {
    val books = search("TWO AND NOT author:[r TO t]")
    assertEquals(books, Right(List(mice)))
  }

  test("AND with field TermRange") {
    val books = search("TWO AND author:[a TO e]")
    assertEquals(books, Right(List(mice, fish)))
  }

  test("implicit OR with field TermRange") {
    val books = search("two author:[a TO c]")
    assertEquals(books, Right(List(mice, peter, fish)))
  }

  test("explicit OR with field TermRange") {
    val books = search("two OR author:[a TO c]")
    assertEquals(books, Right(List(mice, peter, fish)))
  }

  test("field Group") {
    val books = search("author:(potter seuss)")
    assertEquals(books, Right(List(peter, mice, fish, eggs)))
  }

  test("field Group with NOT") {
    val books = search("author:(potter seuss) AND NOT eggs")
    assertEquals(books, Right(List(peter, mice, fish)))
  }

  test("nested Groups") {
    val books = search("(eggs AND ham AND author:(potter SEUSS))")
    assertEquals(books, Right(List(eggs)))
  }

  test("single term phrase query (original casing)") {
    val books = search("\"Eggs\"")
    assertEquals(books, Right(List(eggs)))
  }

  test("single term phrase query") {
    val books = search("\"eggs\"")
    assertEquals(books, Right(List(eggs)))
  }

  test("multi term phrase query") {
    val books = search("\"Green Eggs\"")
    assertEquals(books, Right(List(eggs)))
  }

  test("regex") {
    val books = search("/e(r|e)/")
    assertEquals(books, Right(List(peter, eggs)))
  }

  test("regex fail") {
    val q = search("/[a/")
    val err = "Invalid regex query TermRegex([a)"
    assertEquals(
      q,
      Left(err)
    )
  }

}
