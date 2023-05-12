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
package benchmarks

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

import pink.cozydev.protosearch.analysis.TokenStream.tokenizeSpaceL
import pink.cozydev.lucille.MultiQuery
import cats.data.NonEmptyList
import pink.cozydev.lucille.Query.{Group, Or, Prefix, Term}

/** To run the benchmark from within sbt:
  *
  * jmh:run -i 10 -wi 10 -f 2 -t 1 pink.cozydev.protosearch.benchmarks.IndexingBenchmark
  *
  * Which means "10 iterations", "10 warm-up iterations", "2 forks", "1 thread". Please note that
  * benchmarks should be usually executed at least in 10 iterations (as a rule of thumb), but
  * more is better.
  */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class QueryBenchmark {

  var index: Index = _
  @Setup
  def setup(): Unit = {
    val docs: List[List[String]] =
      List(
        tokenizeSpaceL("the quick brown fox jumped over the lazy cat"),
        tokenizeSpaceL("the very fast cat jumped across the room"),
        tokenizeSpaceL("a lazy cat sleeps all day"),
      )
    index = Index(docs)
  }

  @Benchmark
  def queryIndex(): Boolean = {
    val query = MultiQuery(
      Term("fast"),
      Group(NonEmptyList.one(Or(NonEmptyList.of(Term("c"), Prefix("c"))))),
    )
    BooleanRetrieval(index).search(query).isRight
  }

}
