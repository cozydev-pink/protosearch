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
    val terms: Array[String], // TODO remove
    val postings: Array[PositionalPostingsReader],
    val relativePositions: Array[Int],
) extends Iterator[Int] {

  // TODO optimized ordering
  // We could check for doc matches with postings ordered by term frequency
  // The most infrequent terms should be checked first to enable quick short circuiting
  // However, Position Postings need to be ordered according to their relativePositions
  // i.e. we need to check the first posting first...

  // TODO remove
  private[this] var iterationLimit = 0

  private[this] var currDocId: Int = 0

  /* Tests whether we have more matching documents */
  def hasNext: Boolean = currDocId != -1

  /* Produces the next matching docId, returns -1 if no more matches */
  def next(): Int = {
    while (!allDocsMatch(currDocId)) {
      // bail early if no more matching docs
      if (next(currDocId) == -1) return -1
      // iterate until positional match
      var currStartPosition = 0
      while (currStartPosition != -1 && !allPositionsMatch)
        currStartPosition = nextPosition()
    }
    val res = currDocId
    // prepare for next, increment current docId if we're not done
    if (currDocId != -1) {
      currDocId += 1
    }
    if (allPositionsMatch) res else -1
  }

  /** Returns true if all postings match `docId` */
  def allDocsMatch(docId: Int): Boolean =
    firstNonMatching(docId) == -1

  /** Returns first index that does not match `docId` */
  def firstNonMatching(docId: Int): Int =
    postings.indexWhere(p => p.currentDocId != docId)

  /** Returns true if all postings are in positional match */
  def allPositionsMatch: Boolean =
    firstNonPositionMatching == -1

  /** Returns the index of the first posting that is not in a positional match
    * with the postings preceding it according to the constraints set by `relativePositions`.
    */
  def firstNonPositionMatching: Int = {
    iterationLimit += 1
    require(iterationLimit <= 100, "Exceeded iteration limit on positionsMatch")
    // TODO handle "slop" / error distance
    // Check that each position is satisfying it's relative position
    if (postings.size < 2)
      -1
    else {
      val firstIndexNotInMatchWithNextIndex =
        postings.map(_.currentPosition).zipWithIndex.sliding(2).indexWhere { pair =>
          val ((p1, i1), (p2, i2)) = (pair(0), pair(1))
          val r1 = relativePositions(i1)
          val r2 = relativePositions(i2)
          r2 - r1 != p2 - p1
        }
      // Because of the `sliding(2)` we want to increment by one if not -1
      // This let's us target the "NextIndex" we're not in match with
      if (firstIndexNotInMatchWithNextIndex == -1)
        -1
      else firstIndexNotInMatchWithNextIndex + 1
    }
  }

  def next(docId: Int): Int = {
    // Advance currDocId to at least docId target
    currDocId = docId
    // Iterate over all postings until they match
    while (!allDocsMatch(currDocId)) {
      val i = firstNonMatching(currDocId)
      val newDocId = postings(i).nextDoc(currDocId)
      if (newDocId < currDocId)
        // posting has no docIds at or above currDocId, bail
        return -1
      currDocId = newDocId
    }
    currDocId
  }

  def nextPosition(): Int = {
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
    val relativePositions = (0 to terms.size).toArray
    val maybePostings = terms.toList.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps =>
      new PositionalIter(terms, ps.map(_.reader()).toArray, relativePositions)
    )
  }
}
