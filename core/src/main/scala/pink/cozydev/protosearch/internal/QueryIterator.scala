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
  def currentScore: Float = ???
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
  def currentScore: Float = ???

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
