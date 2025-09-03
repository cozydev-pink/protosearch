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

import pink.cozydev.protosearch.internal.TermDictionary

trait Index {
  def numDocs: Int
  def numTerms: Int

  def docCount(term: String): Int
  def docsWithTerm(term: String): Iterator[Int]
  def docsForRange(left: String, right: String): Iterator[Int]
  def docsForPrefix(prefix: String): Iterator[Int]

  // Preliminary Scoring support
  def termDict: TermDictionary
  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Float)]

}
