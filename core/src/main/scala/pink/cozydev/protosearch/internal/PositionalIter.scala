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
import cats.syntax.all._

class PositionalIter(
    val postings: Array[PositionalPostingsReader],
    val relativePositions: Array[Int],
) extends QueryIterator {

  // TODO optimized ordering
  // We could check for doc matches with postings ordered by term frequency
  // The most infrequent terms should be checked first to enable quick short circuiting
  // However, Position Postings need to be ordered according to their relativePositions
  // i.e. we need to check the first posting first...

  private[this] var currDocId: Int = 0

  def currentDocId: Int = currDocId
  def currentScore: Float = ???

  /* First attempts to advance all postings to matching docId, and only then
   * attempts to find a matching position.
   */
  def advance(docId: Int) =
    if (currDocId == -1) -1 else if (docId <= currDocId) currDocId else advanceAllDocs(docId)

  /** Returns true if all postings match `docId` */
  def allDocsMatch(docId: Int): Boolean =
    firstNonMatching(docId) == -1

  /** Returns first index that does not match `docId` */
  def firstNonMatching(docId: Int): Int =
    postings.indexWhere(p => p.currentDocId != docId)

  /** Returns true if all postings are in positional match */
  private def allPositionsMatch: Boolean =
    firstNonPositionMatching == -1

  /** Returns the index of the first posting that is not in a positional match
    * with the postings preceding it according to the constraints set by `relativePositions`.
    */
  def firstNonPositionMatching: Int =
    // TODO handle "slop" / error distance
    if (postings.size < 2) -1
    else NextPositionToMatch.nextNotInOrderWithLargest(postings.map(_.currentPosition))

  private def advanceAllDocs(docId: Int): Int = {
    // Advance currDocId to at least docId target
    currDocId = docId
    // Iterate over all postings until they match
    while (!allDocsMatch(currDocId)) {
      val i = firstNonMatching(currDocId)
      val newDocId = postings(i).advance(currDocId)
      if (newDocId < currDocId) {
        // posting has no docIds at or above currDocId, bail
        currDocId = -1
        return -1
      }
      currDocId = newDocId
    }
    if (!allDocsMatch(currDocId)) {
      // after while loop, not all docs match $currDocId, setting -1
      currDocId = -1
    } else {
      // All docs match, now let's check positions!
      var currStartPosition = 0
      while (currStartPosition != -1 && !allPositionsMatch)
        currStartPosition = nextPositionAllDocs()
      if (currStartPosition == -1) {
        // we didn't find matching positions on this doc, look for a new doc
        return advanceAllDocs(currDocId + 1)
      }
    }
    currDocId
  }

  // advance all postings to next matching position
  def nextPositionAllDocs(): Int = {
    var currPosition = 0
    // Iterate until all postings in positional match
    while (!allPositionsMatch) {
      val i = firstNonPositionMatching
      val posting = postings(i)
      if (posting.hasNextPosition) {
        currPosition = posting.nextPosition()
      } else {
        // we're not in positional match
        // and we have no more positions, bail
        return -1
      }
    }
    currPosition
  }

}
object PositionalIter {
  // TODO do we want this to live here? It makes this file depend on Lucille and the index
  def exact(index: PositionalIndex, q: Query.Phrase): Option[PositionalIter] = {
    val terms = q.str.split(" ")
    val relativePositions = (1 to terms.size).toArray
    val maybePostings = terms.toList.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps => new PositionalIter(ps.map(_.reader()).toArray, relativePositions))
  }
}
