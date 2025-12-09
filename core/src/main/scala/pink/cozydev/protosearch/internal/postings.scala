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

import pink.cozydev.protosearch.ScoreFunction

/** A non-empty array of postings for a single term. */
final class FrequencyPostingsList private[internal] (private val postings: Array[Int]) {

  def queryIterator(scorer: ScoreFunction): QueryIterator = new QueryIterator {
    private val max = postings.size
    private val numDocs = postings.size / 2
    private var currDocId = 0
    private var docIndex = -2
    def currentDocId: Int = currDocId

    def currentScore: Float =
      scorer(postings(docIndex + 1), currDocId, numDocs)

    private def hasNext = (docIndex + 2) < max || docIndex == -2

    def advance(docId: Int): Int =
      if (currDocId == -1) -1
      else if (docId <= currDocId) currDocId
      else {
        // docId > currDocId, let's try and advance
        while (docId > currDocId && hasNext) {
          docIndex += 1 + 1
          currDocId = postings(docIndex)
        }
        if (currDocId < docId) -1 else currDocId
      }

  }

}
object FrequencyPostingsList {
  import scodec.{Codec, codecs}
  import scodec.codecs._
  import pink.cozydev.protosearch.codecs.IndexCodecs

  val codec: Codec[FrequencyPostingsList] = {
    val header = "typeFlag" | constant(0x41)

    header ~> IndexCodecs
      .arrayOfN(codecs.vint, codecs.vint)
      .xmap(
        arr => new FrequencyPostingsList(arr),
        p => p.postings,
      )
  }
}
final class FrequencyPostingsBuilder {

  // cat ->          // "cat" appears in 2 documents
  // 4, 3,           // doc 4, 3 times
  // 7, 1,           // doc 7, once
  // cat array:
  // 4, 3, 7, 1
  private[this] var buffer = new Array[Int](16)
  private[this] var length = 0

  // Keeps track of the current document ID
  private[this] var currentDocId = -1

  // Keeps track of the index for the current document's term frequency
  private[this] var freqIndex = -1

  def addTerm(docId: Int): Unit = {
    checkAndGrow()
    if (docId == currentDocId) {
      // another occurrence in the same doc
      // increase frequency, add new position at end, increase length
      buffer(freqIndex) += 1
    } else {
      // new doc, set docId, freq
      currentDocId = docId
      buffer(length) = docId
      freqIndex = length + 1
      buffer(freqIndex) = 1
      length += 2
    }
  }

  private[this] def checkAndGrow(): Unit =
    // We always keep room for another docID and freq pair
    if (length >= buffer.length - 2) {
      val buffer2 = new Array[Int](length * 2)
      System.arraycopy(buffer, 0, buffer2, 0, length)
      buffer = buffer2
    }

  def toFrequencyPostingsList: FrequencyPostingsList = {
    require(length > 0, "Cannot make empty FrequencyPostingsList")
    new FrequencyPostingsList(buffer.slice(0, length))
  }
}

/** A stateful reader for `PositionalPostingsList`s, tracking the `currentDocId` and
  * `currentPosition` as it iterates through the postings.
  */
private[internal] abstract class PositionalPostingsReader {
  def currentDocId: Int
  def currentPosition: Int

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def advance(docId: Int): Int

  def currentFrequency: Int
  def hasNext: Boolean
  def nextDoc(): Int

  def hasNextPosition: Boolean

  /** Advances and returns the next position if possible, returns -1 if there are no remaining positions.
    *
    * @return new `currentPosition` value
    */
  def nextPosition(): Int
  def nextPosition(target: Int): Int
}

/** A non-empty array of postings for a single term. */
final class PositionalPostingsList private[internal] (private val postings: Array[Int]) {

  def queryIterator(scorer: ScoreFunction): QueryIteratorWithPositions =
    new QueryIteratorWithPositions {
      private[this] var docIndex = 0
      private[this] var posIndex = 2
      private[this] val numDocs = postings.size / 3 // TODO WRONG, need to save numDocs in array

      def currentDocId: Int = postings(docIndex)
      def currentFrequency: Int = postings(docIndex + 1)
      def currentScore: Float = scorer(currentFrequency, currentDocId, numDocs)
      def currentPosition: Int = postings(posIndex)

      override def toString(): String =
        s"PositionalPostingsReader(i=$docIndex, posIndex=$posIndex, currentDocId=$currentDocId, currentPosition=$currentPosition\n  positions=${postings.toList})"

      def hasNext: Boolean =
        (docIndex + 2) < postings.size &&
          (docIndex + 1 + postings(docIndex + 1) + 1) < postings.size

      def nextDoc(): Int = {
        docIndex += 1 + currentFrequency + 1
        posIndex = docIndex + 2
        currentDocId
      }

      def advance(docId: Int): Int =
        if (currentDocId == -1) -1
        else if (docId <= currentDocId) currentDocId
        else {
          val res = {
            var newDocId = currentDocId
            while (currentDocId < docId && hasNext)
              newDocId = nextDoc()
            newDocId
          }
          if (currentDocId < docId) -1 else res
        }

      def hasNextPosition: Boolean =
        // less than or equal to catch the very last position
        posIndex <= docIndex + currentFrequency

      def nextPosition(): Int = {
        posIndex += 1
        currentPosition
      }

      def nextPosition(target: Int): Int = {
        var newPos = currentPosition
        while (currentPosition < target && hasNextPosition)
          newPos = nextPosition()
        newPos
      }
    }

}
object PositionalPostingsList {
  import scodec.{Codec, codecs}
  import scodec.codecs._
  import pink.cozydev.protosearch.codecs.IndexCodecs

  val codec: Codec[PositionalPostingsList] = {
    val header = "typeFlag" | constant(0x42)

    header ~> IndexCodecs
      .arrayOfN(codecs.vint, codecs.vint)
      .xmap(
        arr => new PositionalPostingsList(arr),
        p => p.postings,
      )
  }
}

final class PositionalPostingsBuilder {
  // TODO do we want to encode the number of doc matches?

  // cat ->            // "cat" appears in 2 documents
  // 4, 3, 7, 12, 47   // doc 4, 3 occurrences at positions 7, 12, and 47
  // 7, 1, 3           // doc 7, 1 occurrence at position 3
  // cat array:
  // 4, 3, 7, 12, 47, 7, 1, 3
  private[this] var buffer = new Array[Int](16)
  private[this] var length = 0

  // Keeps track of the current document ID
  private[this] var currentDocId = -1

  // Keeps track of the index for the current document's term frequency
  private[this] var freqIndex = -1

  def addTermPosition(docId: Int, position: Int): Unit = {
    checkAndGrow()
    if (docId == currentDocId) {
      // another occurrence in the same doc
      // increase frequency, add new position at end, increase length
      buffer(freqIndex) += 1
      buffer(length) = position
      length += 1
    } else {
      // new doc, set docId, freq, first position
      currentDocId = docId
      buffer(length) = docId
      freqIndex = length + 1
      buffer(freqIndex) = 1
      buffer(length + 2) = position
      length += 3
    }
  }

  private[this] def checkAndGrow(): Unit =
    // We always keep room for another docID, freq, position triple
    if (length >= buffer.length - 3) {
      val buffer2 = new Array[Int](length * 2)
      System.arraycopy(buffer, 0, buffer2, 0, length)
      buffer = buffer2
    }

  def toPositionalPostingsList: PositionalPostingsList = {
    require(length > 0, "Cannot make empty PositionalPostingsList")
    new PositionalPostingsList(buffer.slice(0, length))
  }
}
