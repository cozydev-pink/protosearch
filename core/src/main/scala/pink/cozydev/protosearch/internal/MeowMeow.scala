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

  private var recursionLim = 0

  private var currDocId: Int = 0
  private var currStartPosition: Int = 0

  def allDocsMatch(n: Int): Boolean =
    // println(s"allDocsMatch($n): " + printAllPostings)
    postings.forall(p => p.currentDocId() == n)

  def positionArr: Array[Int] = postings.map(p => p.currentPosition())

  // TODO for assume no "slop"
  def allPositionsMatch: Boolean = {
    val res = positionsMatch == -1
    // println(s"allPositionsMatch=$res positionsMatch=$positionsMatch")
    res
  }

  def positionsMatch: Int = {
    recursionLim += 1
    if (recursionLim >= 100) {
      println(s"EXCEEDED RECURSION LIMIT")
      throw new IllegalStateException
    }
    // println("-- positions: " + printAllPostingPositions)
    // Check that each position is satisfying it's relative position
    if (positionArr.size >= 2) {
      positionArr.zipWithIndex.sliding(2).indexWhere { pair =>
        val ((p1, i1), (p2, i2)) = (pair(0), pair(1))
        val r1 = relativePositions(i1)
        val r2 = relativePositions(i2)
        // println(s"r2=$r2 r1=$r1 p2=$p2 p1=$p1")
        r2 - r1 != p2 - p1
      }
      // only one position, so we must be in match
    } else -1
  }

  def printAllPostings: String =
    postings
      .map(p => p.currentDocId())
      .zipWithIndex
      .map { case (docId, i) => s"${terms(i)}:$docId" }
      .mkString(", ")

  def printAllPostingPositions: String =
    postings
      .map(p => (p.currentDocId(), p.currentPosition()))
      .zipWithIndex
      .map { case ((docId, posId), i) => s"${terms(i)}:$docId,$posId" }
      .mkString("  ")

  def printPosting(i: Int): String =
    s"i=$i term=${terms(i)}, posting=${postings(i)}"

  def hasNext(): Boolean = hasNextDoc && hasNextPosition
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
    var i = 0
    currDocId = docId
    // advance all postings until they are in match position
    while (i < postings.size && !allDocsMatch(currDocId)) {
      println(printPosting(i))
      val posting = postings(i)
      val di = posting.nextDoc(currDocId)
      if (di != currDocId) {
        // that posting didn't have a match at currDocId
        println(s"no match for term '${terms(i)}' with docID=$currDocId")
        if (di > currDocId) {
          println(s"term '${terms(i)}' has other matches, update currDocId, go to top of loop")
          i = 0
          currDocId = di
        } else {
          println(s"early exit, term '${terms(i)}' has no other matches")
          currDocId = -1
          return -1
        }
      } else {
        i += 1
      }
    }
    if (!allDocsMatch(currDocId)) {
      currDocId = -1
    }
    println(s"YAY, finished doc-matching while-loop with currDocId=$currDocId")
    currDocId
  }

  // all docs match, try to match positions
  // Maybe a two iterator approach makes sense
  // One iterator matches docs
  // second iterator on iterates over the doc-matching cases
  // and it only emits values if the positions match
  // TODO NEXT do this ^^^ two iterators!
  def nextPosition(target: Int): Int = {
    var i = positionsMatch
    currStartPosition = target
    while (i < postings.size && !allPositionsMatch) {
      println(s"nextPosition (i=$i): " + printAllPostingPositions)
      val posting = postings(i)
      val pi = posting.nextPosition()
      // Have we made progress? Or do we need the next position on this posting?
      val pm = positionsMatch
      if (pm != -1) {
        i = pm
      }
      // TODO need to use pm info to retarget i
      println(s"+ i=$i, term=${terms(i)}, positionsMatch=$pm, pi=$pi")
      if (pi != -1 && i > pm) {
        // we have more positions, and we have not made enough progress
        println(s"!! no pos match for term '${terms(i)}' with pi=$pi")
      } else {
        if (pi == -1) {
          println(s"no other matches")
          currStartPosition = -1
          return -1
        }
        // we have made enough progress, set i to the current positionsMatch + 1
        println(s"made progress, i=$i, positionsMatch=$pm, pi=$pi")
        i = pm + 1
      }
    }
    if (!allPositionsMatch) {
      println(s"!! positions not in match")
      currStartPosition = -1
    }
    println(s"!! finished pos-matching, currStartPosition=$currStartPosition")
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
