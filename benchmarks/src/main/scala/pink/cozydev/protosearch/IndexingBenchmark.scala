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

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import fs2.text
import fs2.io.file.{Files, Path}

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

import pink.cozydev.protosearch.analysis.TokenStream.tokenizeSpaceL

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
class IndexingBenchmark {

  var doc: String = _
  @Setup
  def setup(): Unit =
    doc = Files[IO]
      .readAll(Path("../LICENSE"))
      .through(text.utf8.decode)
      .compile
      .string
      .unsafeRunSync()

  @Benchmark
  def buildIndex(): Index =
    Index(tokenizeSpaceL(doc) :: Nil)

}
