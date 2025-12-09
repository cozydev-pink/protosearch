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
import pink.cozydev.protosearch.PositionalIndex
import pink.cozydev.protosearch.ScoreFunction
import cats.syntax.all._

abstract class QueryIterator {
  def currentDocId: Int
  def currentScore: Float

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def advance(docId: Int): Int

  def docs: Iterator[Int] = {
    var head: Int = advance(1)
    if (head == -1) Iterator.empty
    else
      new Iterator[Int] {
        var hasNext: Boolean = true
        def next(): Int = {
          val res = head
          head = advance(currentDocId + 1)
          if (head == -1) {
            hasNext = false
          }
          res
        }

      }
  }

  def scoredDocs: Iterator[(Int, Float)] = {
    var head: Int = advance(1)
    if (head == -1) Iterator.empty
    else
      new Iterator[(Int, Float)] {
        var hasNext: Boolean = true
        def next(): (Int, Float) = {
          val res = (head, currentScore)
          head = advance(currentDocId + 1)
          if (head == -1) {
            hasNext = false
          }
          res
        }

      }
  }
}
abstract class QueryIteratorWithPositions extends QueryIterator {
  def currentPosition: Int
  def hasNextPosition: Boolean
  def nextPosition(): Int
  def nextPosition(target: Int): Int
}
object QueryIterator {
  def empty = new NoMatchQueryIterator
}

class BoostQueryIterator(qi: QueryIterator, boost: Float) extends QueryIterator {
  def currentDocId = qi.currentDocId
  def currentScore = qi.currentScore * boost
  def advance(docId: Int) = qi.advance(docId)
}

class ConstantScoreQueryIterator(qi: QueryIterator, score: Float) extends QueryIterator {
  def currentDocId = qi.currentDocId
  val currentScore = score
  def advance(docId: Int) = qi.advance(docId)
}

class NoMatchQueryIterator extends QueryIterator {
  val currentDocId = -1
  val currentScore = 0.0f
  def advance(docId: Int) = -1
  override def docs: Iterator[Int] = Iterator.empty
}

class NotQueryIterator(qi: QueryIterator, max: Int) extends QueryIterator {
  var currDocId = 0
  var innerCurrDocId = qi.advance(1)

  def currentDocId = currDocId
  def currentScore = qi.currentScore

  // We match every doc that `qi` doesn't
  // If `qi` is exhausted, we match docIds up to `max`
  def advance(docId: Int): Int = {
    currDocId = docId
    if (currDocId > max || currDocId == -1) {
      // Bail early
      return -1
    }
    if (docId < innerCurrDocId) currDocId
    else {
      // Being asked about a docId we can't match
      // or don't yet know how `qi` matches, so we advance `qi`
      // until we find a docId it doesn't match
      var target = docId
      innerCurrDocId = qi.advance(target)
      while (innerCurrDocId != -1 && currDocId < innerCurrDocId) {
        innerCurrDocId = qi.advance(target)
        if (innerCurrDocId > docId) {
          return currDocId
        }
        target += 1
      }
      if (currDocId == innerCurrDocId) advance(docId + 1) else currDocId
    }
  }

}
class AndIter(
    things: Array[QueryIterator]
) extends QueryIterator {
  private var currDocId: Int = 0

  def currentDocId: Int = currDocId
  def currentScore: Float =
    things.map(_.currentScore).sum
  def advance(docId: Int): Int = {
    val target = things(0).advance(docId)
    currDocId = target
    if (target == -1) -1
    else {
      var newTarget = target
      var i = 1
      while (i < things.size) {
        newTarget = things(i).advance(target)
        if (newTarget < currDocId)
          // post has no docIds at or above currDocId, bail
          return -1
        if (newTarget == target) {
          // matched, continue
          i += 1
        } else {
          // found a match at higher docId, restart with new target
          return advance(newTarget)
        }
      }
      newTarget
    }
  }
}
object AndIter {
  def apply(queries: Seq[QueryIterator]): AndIter =
    new AndIter(queries.toArray)
}

class OrQueryIterator(
    things: Array[QueryIterator],
    minShouldMatch: Int,
) extends QueryIterator {
  private var currDocId: Int = 0

  def currentDocId: Int = currDocId
  def currentScore: Float =
    things.map(_.currentScore).sum

  def advance(docId: Int): Int = if (currDocId == -1) -1
  else {
    currDocId = docId
    // Advance postings, count how many are in match
    var numMatched = 0
    var numDead = 0
    var i = 0
    // Track min docId to know when we've exhausted
    while (i < things.size) {
      val newTarget = things(i).advance(currDocId)
      if (newTarget < currDocId)
        // posting has no docIds at or above currDocId, but others might
        numDead += 1
      if (numDead >= things.size) {
        currDocId = -1
        return -1
      }
      if (newTarget == currDocId) {
        numMatched += 1
      }
      i += 1
    }
    if (numMatched >= minShouldMatch) currDocId else advance(currDocId + 1)
  }
}
object OrQueryIterator {
  def apply(queries: Array[QueryIterator], minShouldMatch: Int): OrQueryIterator =
    if (queries.size < 1)
      throw new IllegalArgumentException("queries array must not be empty")
    else if (minShouldMatch < 0)
      throw new IllegalArgumentException("minShouldMatch must be positive")
    else
      new OrQueryIterator(queries, minShouldMatch)

  def apply(queries: Seq[QueryIterator], minShouldMatch: Int): OrQueryIterator =
    apply(queries.toArray, minShouldMatch)
}

class PhraseIterator(
    val postings: Array[QueryIteratorWithPositions],
    val relativePositions: Array[Int],
) extends QueryIterator {

  // TODO optimized ordering
  // We could check for doc matches with postings ordered by term frequency
  // The most infrequent terms should be checked first to enable quick short circuiting
  // However, Position Postings need to be ordered according to their relativePositions
  // i.e. we need to check the first posting first...

  private[this] var currDocId: Int = 0

  def currentDocId: Int = currDocId
  def currentScore: Float = 1.0f

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
object PhraseIterator {
  def exact(index: PositionalIndex, terms: List[String]): Option[PhraseIterator] = {
    val relativePositions = (1 to terms.size).toArray
    val maybePostings = terms.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps =>
      new PhraseIterator(ps.map(_.queryIterator(ScoreFunction.noScore)).toArray, relativePositions)
    )
  }
}
