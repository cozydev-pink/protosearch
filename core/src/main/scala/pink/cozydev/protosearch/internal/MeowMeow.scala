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

// This is a mutable structure that callers will repeated call `next()` on.
abstract class MeowMeow {
  // This is going to represent a node in the "Query Tree"
  // Where the whole tree, and it's various nodes acts like an iterator
  // This iterator both matches and can score documents

  // A parent iterator can request the next match and specify a minimum matching
  // docID to consider, we can thus skip over other documents that we might match,
  // but which other iterators will not match
  def next(docId: Int): Int
}

// TODO Where does this class start?
// We could take in the `Query.Phrase`, or the list of terms and positions
// Or perhaps we take in our own Query representation?
class PhraseMeowMeow(
    val terms: Array[String], // TODO remove
    val postings: Array[PositionalPostingsReader],
    val relativePositions: Array[Int],
) extends MeowMeow
    with Iterator[Int] {
  // TODO can this not be a concrete collection?
  // Could it not just be pointers into the tfData?
  // The ordering here perhaps matters. I think we want them ordered by frequency or length.
  // The most infrequent terms should be checked first to enable quick short circuiting
  // Position Postings need to be ordered according to their relativePositions
  // i.e. we need to check the first posting first...

  private[this] var iterationLimit = 0

  private[this] var currDocId: Int = 0
  private[this] var currStartPosition: Int = 0

  private def printAllPostings: String =
    postings
      .map(p => p.currentDocId)
      .zipWithIndex
      .map { case (docId, i) => s"${terms(i)}:$docId" }
      .mkString(", ")

  private def printAllPostingPositions: String =
    postings
      .map(p => (p.currentDocId, p.currentPosition))
      .zipWithIndex
      .map { case ((docId, posId), i) => s"${terms(i)}:$docId,$posId" }
      .mkString("  ")

  private def printPosting(i: Int): String =
    s"i=$i term=${terms(i)}, posting=${postings(i)}"

  def allDocsMatch(n: Int): Boolean =
    docsMatch(n) == -1

  def docsMatch(n: Int): Int =
    postings.indexWhere(p => p.currentDocId != n)

  // TODO for assume no "slop"
  def allPositionsMatch: Boolean = {
    val res = positionsMatch == -1
    // println(s"allPositionsMatch=$res positionsMatch=$positionsMatch")
    res
  }

  /** Returns the index of the first posting that is not in a positional match
    * with the postings preceding it according to the constraints set by `relativePositions`.
    */
  def positionsMatch: Int = {
    iterationLimit += 1
    require(iterationLimit <= 100, "Exceeded iteration limit on positionsMatch")
    // Check that each position is satisfying it's relative position
    if (postings.size < 2)
      -1
    else
      postings.map(_.currentPosition).zipWithIndex.sliding(2).indexWhere { pair =>
        val ((p1, i1), (p2, i2)) = (pair(0), pair(1))
        val r1 = relativePositions(i1)
        val r2 = relativePositions(i2)
        r2 - r1 != p2 - p1
      }
  }

  def hasNext: Boolean = hasNextDoc && hasNextPosition
  def hasNextDoc: Boolean = currDocId != -1
  def hasNextPosition: Boolean = currStartPosition != -1

  def next(): Int = {
    while (!allDocsMatch(currDocId)) {
      val doc = nextDoc()
      if (doc == -1) return -1
      while (currStartPosition != -1 && !allPositionsMatch) {
        val _ = nextPosition(currStartPosition)
      }
    }
    val res = currDocId
    if (currDocId != -1) {
      currDocId += 1
    }
    if (allPositionsMatch) res else -1
  }

  def nextDoc(): Int = {
    require(hasNext, "We have no next document!")
    val res = next(currDocId)
    res
  }

  def next(docId: Int): Int = {
    // Advance currDocId to at least docId target
    currDocId = docId
    // Iterate over all postings until they match
    while (!allDocsMatch(currDocId)) {
      val i = docsMatch(currDocId)
      val newDocId = postings(i).nextDoc(currDocId)
      if (newDocId < currDocId)
        // posting has no docIds at or above currDocId, bail
        return -1
      currDocId = newDocId
    }
    currDocId
  }

  def nextPosition(target: Int): Int = {
    var i = positionsMatch + 1
    currStartPosition = target
    while (i < postings.size && !allPositionsMatch) {
      // println(s"nextPosition (i=$i): " + printAllPostingPositions)
      val posting = postings(i)
      val pi = posting.nextPosition()
      // Have we made progress? Or do we need the next position on this posting?
      val pm = positionsMatch
      // println(s"+ i=$i, term=${terms(i)}, positionsMatch=$pm, pi=$pi")
      if (pi != -1 && i > pm) {
        // we have more positions, and we have not made enough progress
        // println(s"!! no pos match for term '${terms(i)}' with pi=$pi")
      } else {
        if (pi == -1) {
          // println(s"no other matches (pi == -1)")
          currStartPosition = -1
          return -1
        }
        // we have made enough progress, set i to the current positionsMatch + 1
        // println(s"made progress, i=$i, positionsMatch=$pm, pi=$pi")
      }
      i = pm + 1
    }
    if (!allPositionsMatch) {
      // println(s"!! positions not in match")
      currStartPosition = -1
    }
    // println(s"!! finished pos-matching, currStartPosition=$currStartPosition")
    currStartPosition
  }

}
object PhraseMeowMeow {
  // TODO do we want this to live here? It makes this file depend on Lucille and the index
  def exact(index: PositionalIndex, q: Query.Phrase): Option[PhraseMeowMeow] = {
    val terms = q.str.split(" ")
    val relativePositions = (0 to terms.size).toArray
    val maybePostings = terms.toList.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps =>
      new PhraseMeowMeow(terms, ps.map(_.reader()).toArray, relativePositions)
    )
  }
}
