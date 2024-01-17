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

private[internal] abstract class PositionalPostingsReader {
  def currentDocId(): Int
  def currentPosition(): Int
  def hasNext(): Boolean
  def nextDoc(): Int
  def nextDoc(docId: Int): Int
  def nextPosition(): Int
  def nextPosition(position: Int): Int
}

final class PositionalPostingsList private[internal] (val postings: Array[Int]) {

  def reader(): PositionalPostingsReader = new PositionalPostingsReader {
    // TODO require non-empty?
    require(postings.size >= 3, "PositionalPostingsList must have at least one entry")

    var i = 0
    var currDocId = postings(i)
    var currDocFreq = postings(i + 1)
    var currPosition = postings(i + 2)

    override def toString(): String =
      s"PositionalPostingsReader(i=$i, currentDocId=$currDocId, currentPosition=$currPosition)\n  ${postings.toList}"

    def hasNext: Boolean =
      (i + 2) < postings.size &&
        (i + 1 + postings(i + 1) + 1) < postings.size
    def currentDocId(): Int = currDocId

    def currentPosition(): Int = currPosition

    def nextDoc(): Int = {
      i += 1 + currDocFreq + 1
      currDocId = postings(i)
      currDocFreq = postings(i + 1)
      currPosition = postings(i + 2)
      currDocId
    }

    def nextDoc(docId: Int): Int = {
      while (currDocId < docId && hasNext) {
        println(s"nextDoc while: i=$i currDocId=$currDocId, docId=$docId, currDocFreq=$currDocFreq")
        i += 1 + currDocFreq + 1
        currDocId = postings(i)
        currDocFreq = postings(i + 1)
        currPosition = postings(i + 2)
      }
      currDocId
    }

    def nextPosition(): Int = ???

    def nextPosition(position: Int): Int = ???

  }

  def docs: Iterator[Int] = new Iterator[Int] {
    var i = 0
    def hasNext: Boolean =
      (i + 2) < postings.size &&
        // TODO why is this one <=
        (i + 1 + postings(i + 1) + 1) <= postings.size
    def next(): Int = {
      val docId = postings(i)
      val freq = postings(i + 1)
      // jump ahead one to the freq, then the freq amount, then one more to the next docId
      i += 1 + freq + 1
      docId
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

  // TODO how does this work for first addition?
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

  def toPositionalPostingsList: PositionalPostingsList =
    new PositionalPostingsList(buffer.slice(0, length))
}
