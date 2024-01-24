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

/** A stateful reader for `PositionalPostingsList`s, tracking the `currentDocId` and
  * `currentPosition` as it iterates through the postings.
  */
private[internal] abstract class PositionalPostingsReader {
  def currentDocId: Int
  def currentPosition: Int
  def hasNext: Boolean
  def nextDoc(): Int

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def nextDoc(docId: Int): Int

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

  def reader(): PositionalPostingsReader = new PositionalPostingsReader {
    private[this] var docIndex = 0
    private[this] var posIndex = 2

    private[this] def currDocFreq = postings(docIndex + 1)

    def currentDocId: Int = postings(docIndex)
    def currentPosition: Int = postings(posIndex)

    override def toString(): String =
      s"PositionalPostingsReader(i=$docIndex, posIndex=$posIndex, currentDocId=$currentDocId, currentPosition=$currentPosition\n  positions=${postings.toList})"

    def hasNext: Boolean =
      (docIndex + 2) < postings.size &&
        (docIndex + 1 + postings(docIndex + 1) + 1) < postings.size

    def nextDoc(): Int = {
      docIndex += 1 + currDocFreq + 1
      posIndex = docIndex + 2
      currentDocId
    }

    def nextDoc(docId: Int): Int = {
      while (currentDocId < docId && hasNext) nextDoc()
      currentDocId
    }

    def hasNextPosition: Boolean =
      // less than or equal to catch the very last position
      posIndex <= docIndex + currDocFreq

    def nextPosition(): Int = {
      posIndex += 1
      currentPosition
    }

    def nextPosition(target: Int): Int = {
      while (currentPosition < target && hasNextPosition) nextPosition()
      currentPosition
    }
  }

  def docs: Iterator[Int] = new Iterator[Int] {
    // PositionalPostingsList always have at least one element
    var oneAfter: Boolean = true
    def hasNext: Boolean = oneAfter
    val rdr = reader()
    def next(): Int = {
      val res = rdr.currentDocId
      if (rdr.hasNext) rdr.nextDoc() else { oneAfter = false }
      res
    }
  }

}
final class PositionalPostingsBuilder {
  // TODO do we want to encode the number of doc matches?

  // cat, 2 ->         // "cat" appears in 2 documents
  // 4, 3, 7, 12, 47   // doc 4, 3 occurrences at positions 7, 12, and 47
  // 7, 1, 3           // doc 7, 1 occurrence at position 3
  // cat array:
  // 2, 4, 3, 7, 12, 47, 7, 1, 3
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
