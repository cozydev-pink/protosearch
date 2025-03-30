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
    println(s"NOT advance($docId)")
    currDocId = docId
    if (currDocId > max || currDocId == -1) {
      println(s"BAIL currDocId=$currDocId >= max=$max")
      return -1
    }
    if (docId < innerCurrDocId) {
      println(s"NOT docId=$docId < innerCurrDoc=$innerCurrDocId, returning $currentDocId")
      currDocId
    } else {
      println(s"-- NOT docId=$docId >= innerCurrDoc=$innerCurrDocId, currDocId=$currDocId")
      // Being asked about a docId we can't match
      // or don't yet know how `qi` matches, so we advance `qi`
      // until we find a docId it doesn't match
      var target = docId
      val old = innerCurrDocId
      innerCurrDocId = qi.advance(target)
      while (innerCurrDocId != -1 && currDocId < innerCurrDocId) {
        println(
          s"--- NOT while innerCurrDocId=$innerCurrDocId, currDocId=$currDocId, target=$target"
        )
        innerCurrDocId = qi.advance(target)
        if (innerCurrDocId > docId) {
          println(s"--- returning early, innerCurrDocId=$innerCurrDocId, returning $currentDocId")
          return currDocId
        }
        target += 1
      }
      if (currDocId == innerCurrDocId) {
        println(s"done while docId=$docId, innerCurrDoc=currDocId($currDocId), advancing")
        advance(docId + 1)
      } else {
        println(s"done while docId=$docId, returning currDocId")
        currDocId
      }
    }
  }

}
