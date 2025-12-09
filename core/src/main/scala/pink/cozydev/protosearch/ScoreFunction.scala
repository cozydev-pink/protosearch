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

trait ScoreFunction extends ((Int, Int, Int) => Float)
object ScoreFunction {
  val noScore: ScoreFunction = new ScoreFunction {
    def apply(freq: Int, docId: Int, numDocs: Int): Float = 0.0f
  }

  val tfIdf: ScoreFunction = new ScoreFunction {
    def apply(freq: Int, docId: Int, numDocs: Int): Float = {
      val tf = math.log(1.0 + freq)
      val idf = 1.0 / numDocs
      (tf * idf).toFloat
    }
  }
}
