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

import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.lucille.QueryParser
import fixtures.BookIndex

class ScorerSuite extends munit.FunSuite {
  import BookIndex.{Book, allBooks}

  val analyzer = Analyzer.default

  val index = IndexBuilder
    .of[Book](
      (Field("title", analyzer, true, true, true), _.title),
      (Field("author", analyzer, true, true, false), _.author),
    )
    .fromList(allBooks)

  val allDocs: Set[Int] = Set(1, 2, 3, 4)

  val scorer = Scorer(index)
  def score(q: String, docs: Set[Int]): Either[String, List[(Int, Double)]] =
    QueryParser
      .parse(q)
      .flatMap(q => scorer.score(q, docs, 10))

  def ordered(hits: Either[String, List[(Int, Double)]]): List[Int] =
    hits.fold(_ => Nil, ds => ds.map(_._1))

  test("doc with matching term is ordered first") {
    val hits = score("Bad", Set(2))
    assertEquals(ordered(hits), List(2))
  }

  test("no results for docs without matching terms") {
    val hits = score("XYZ", allDocs)
    assertEquals(ordered(hits), Nil)
  }

  test("docs with multiple matches score higher") {
    val hits = score("Tale OR Two", allDocs)
    assertEquals(ordered(hits), List(2, 1, 3))
  }

  test("additional matching queries increases score") {
    val sc1 = score("Tale OR Two", Set(3)).map(_.head._2)
    val sc2 = score("author:Seuss Tale OR Two", Set(3)).map(_.head._2)
    assert(sc1.exists(s1 => sc2.exists(s2 => s2 > s1)))
  }

  test("scores prefix queries") {
    val hits = score("T*", allDocs)
    assertEquals(ordered(hits), List(2, 1, 3))
  }

  test("scores phrase query") {
    val hits = score("\"Two Bad Mice\"", allDocs)
    assertEquals(ordered(hits), List(2))
  }

  test("scorer with topN=1 returns only top doc") {
    val q = "Tale OR Two" // has 3 matches
    val hits = QueryParser.parse(q).flatMap(q => scorer.score(q, allDocs, topN = 1))
    assertEquals(ordered(hits), List(2))
  }

  test("scorer topN can be bigger than number of matches") {
    val q = "Tale OR Two" // has 3 matches
    val hits = QueryParser.parse(q).flatMap(q => scorer.score(q, allDocs, topN = 999))
    assertEquals(ordered(hits), List(2, 1, 3))
  }

}
