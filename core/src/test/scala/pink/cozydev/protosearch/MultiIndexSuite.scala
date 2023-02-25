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

class MultiIndexSuite extends munit.FunSuite {
  import BookIndex.Book

  val peter = Book("The Tale of Peter Rabbit", "Beatrix Potter")
  val mice = Book("The Tale of Two Bad Mice", "Beatrix Potter")
  val fish = Book("One Fish, Two Fish, Red Fish, Blue Fish", "Dr. Suess")
  val eggs = Book("Green Eggs and Ham", "Dr. Suess")

  val allBooks = Vector(peter, mice, fish, eggs)

  val analyzer = Analyzer.default.withLowerCasing
  val index = MultiIndex.apply[Book](
    "title",
    ("title", _.title, analyzer),
    ("author", _.author, analyzer),
  )(allBooks)

  val qAnalyzer = QueryAnalyzer("title", ("title", analyzer), ("author", analyzer))

  def search(qs: String): Either[String, List[Book]] = {
    val q = qAnalyzer.parse(qs)
    // println(s"+++ analyzed query: $q")
    val result = q.flatMap(index.search)
    result.map(hits => hits.map(i => allBooks(i)))
  }

  test("TermQ") {
    val books = search("Bad")
    assertEquals(books, Right(List(mice)))
  }

  test("TermQ lowercased") {
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

  test("AND with field RangeQ") {
    val books = search("TWO AND author:[a TO e]")
    assertEquals(books, Right(List(mice, fish)))
  }

  test("OR with field RangeQ") {
    val books = search("two author:[a TO c]")
    assertEquals(books, Right(List(peter, mice, fish)))
  }

}
