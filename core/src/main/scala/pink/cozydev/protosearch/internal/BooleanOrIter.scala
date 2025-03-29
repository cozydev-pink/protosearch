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

import pink.cozydev.protosearch.FrequencyIndex
import cats.syntax.all._

class BooleanOrIter(
    val postings: Array[FrequencyPostingsReader]
) extends Iterator[Int] {

  private[this] var currDocId: Int = 0

  /* Tests whether we have more matching documents */
  def hasNext: Boolean = currDocId != -1

  /* Produces the next matching docId, returns -1 if no more matches */
  def next(): Int = {
    while (!allDocsMatch(currDocId))
      // bail early if no more matching docs
      if (next(currDocId) == -1) return -1
    val res = currDocId
    // prepare for next, increment current docId if we're not done
    if (currDocId != -1) {
      currDocId += 1
    }
    res
  }

  /** Returns true if all postings match `docId` */
  def allDocsMatch(docId: Int): Boolean =
    firstNonMatching(docId) == -1

  /** Returns first index that does not match `docId` */
  def firstNonMatching(docId: Int): Int =
    postings.indexWhere(p => p.currentDocId != docId)

  def next(docId: Int): Int = {
    var lowestSeen = currDocId
    // Advance currDocId to at least docId target
    currDocId = docId
    // Iterate over all postings, advancing them
    postings.foreach(p => lowestSeen = Math.min(lowestSeen, p.advance(docId)))
    lowestSeen // ???
  }

}
object BooleanOrIter {
  def fromTerms(index: FrequencyIndex, terms: List[String]): Option[BooleanOrIter] = {
    // traverse fails fast on a term outside the corpus
    val maybePostings = terms.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps => new BooleanOrIter(ps.map(_.reader()).toArray))
  }
}
