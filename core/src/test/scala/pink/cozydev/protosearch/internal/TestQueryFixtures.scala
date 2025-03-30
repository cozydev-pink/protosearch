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

package pink.cozydev.protosearch.internal

import pink.cozydev.lucille.Query
import pink.cozydev.protosearch.PositionalIndex
import pink.cozydev.protosearch.analysis.Analyzer

object TestQueryFixtures {

  private val analyzer = Analyzer.default
  val alphaNumIndex = PositionalIndex(
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
      PositionalIter.exact(alphaNumIndex, Query.Phrase(q)) match {
        case Some(iter) => iter
        case None =>
          val msg = s"some terms in query: '$q', could not be found in test index"
          throw new IllegalArgumentException(msg)
      }
  }
}
