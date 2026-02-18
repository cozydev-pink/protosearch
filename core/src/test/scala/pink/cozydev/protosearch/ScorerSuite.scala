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
import pink.cozydev.protosearch.internal.QueryIteratorSearch
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

  val searcher = QueryIteratorSearch(index, ScoreFunction.tfIdf)

  def score(q: String, topN: Int = 10): Either[String, List[(Int, Float)]] =
    QueryParser
      .parse(q)
      .flatMap(q => searcher.scoredSearch(q))
      .map(_.toList.sortBy(-_._2).take(topN))

  def ordered(hits: Either[String, List[(Int, Float)]]): List[Int] =
    hits.fold(_ => Nil, ds => ds.map(_._1))

  def scoreForDoc(q: String, docId: Int): Option[Float] =
    score(q).toOption.flatMap(_.find(_._1 == docId).map(_._2))

  test("doc with matching term is ordered first") {
    val hits = score("Bad")
    assertEquals(ordered(hits), List(2))
  }

  test("no results for docs without matching terms") {
    val hits = score("XYZ")
    assertEquals(ordered(hits), Nil)
  }

  test("docs with multiple matches score higher") {
    val hits = score("Tale OR Two")
    assertEquals(ordered(hits), List(2, 1, 3))
  }

  test("additional matching clauses increase a document's score") {
    val doc3 = 3 // "One Fish, Two Fish..." by Dr. Seuss
    val sc1 = scoreForDoc("Two", doc3)
    val sc2 = scoreForDoc("Two OR author:Seuss", doc3)
    assert(sc1.exists(s1 => sc2.exists(s2 => s2 > s1)))
  }

  test("scores prefix queries") {
    val hits = score("T*")
    assertEquals(ordered(hits), List(2, 1, 3))
  }

  test("scores phrase query") {
    val hits = score("\"Two Bad Mice\"")
    assertEquals(ordered(hits), List(2))
  }

  test("scorer with topN=1 returns only top doc") {
    val q = "Tale OR Two" // has 3 matches
    val hits = score(q, topN = 1)
    assertEquals(ordered(hits), List(2))
  }

  test("scorer topN can be bigger than number of matches") {
    val q = "Tale OR Two" // has 3 matches
    val hits = score(q, topN = 999)
    assertEquals(ordered(hits), List(2, 1, 3))
  }

}
