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

import cats.syntax.all._
import pink.cozydev.protosearch.analysis.{Analyzer, QueryAnalyzer}

class MultiIndexSuite extends munit.FunSuite {
  import BookIndex._

  val analyzer = Analyzer.default.withLowerCasing
  val index = MultiIndex.apply[Book](
    "title",
    (Field("title", analyzer, true, true), _.title),
    (Field("author", analyzer, true, true), _.author),
  )(allBooks)

  val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

  def search(qs: String): Either[String, List[Book]] = {
    val q = qAnalyzer.parse(qs).leftMap(_.toString())
    // println(s"+++ analyzed query: $q")
    val result = q.flatMap(mq => index.search(mq.qs))
    result.map(hits => hits.map(i => allBooks(i)))
  }

  def searchMap(qs: String): Either[String, List[(Int, Map[String, String])]] = {
    val q = qAnalyzer.parse(qs).leftMap(_.toString())
    q.flatMap(mq => index.searchMap(mq.qs))
  }

  test("Term searchMap".only) {
    val books = searchMap("Bad")
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
    val books = search("two AND author:Suess")
    assertEquals(books, Right(List(fish)))
  }

  test("AND with multiple fieldQ") {
    val books = search("title:two AND author:Suess")
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
    assertEquals(books, Right(List(peter, mice, fish)))
  }

  test("explicit OR with field TermRange") {
    val books = search("two OR author:[a TO c]")
    assertEquals(books, Right(List(peter, mice, fish)))
  }

  test("field Group") {
    val books = search("author:(potter suess)")
    assertEquals(books, Right(List(peter, mice, fish, eggs)))
  }

  test("field Group with NOT") {
    val books = search("author:(potter suess) AND NOT eggs")
    assertEquals(books, Right(List(peter, mice, fish)))
  }

  test("nested Groups") {
    val books = search("(eggs AND ham AND author:(potter SUESS))")
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

  test("multi term phrase query fails") {
    val books = search("\"Green Eggs\"")
    assert(books.isLeft)
  }

}
