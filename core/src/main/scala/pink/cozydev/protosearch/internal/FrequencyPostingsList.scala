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

/** A non-empty array of postings for a single term. */
final class FrequencyPostingsList private[internal] (private val postings: Array[Int]) {

  def queryIterator(): QueryIterator = new QueryIterator {
    private val max = postings.size
    private var currDocId = 0
    private var docIndex = -2
    def currentDocId: Int = currDocId

    def currentScore: Float = {
      val currentFreq = postings(docIndex + 1)
      currentFreq.toFloat
    }

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

  def docs: Iterator[Int] = queryIterator().docs

  def frequencyForDocID(docID: Int): Int = {
    var i = 0
    while (i + 1 < postings.size) {
      if (postings(i) == docID) return postings(i + 1)
      i += 2
    }
    -1
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
