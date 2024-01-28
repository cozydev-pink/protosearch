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

package pink.cozydev.protosearch

import scala.collection.mutable.ArrayBuilder
import scala.collection.mutable.ListBuffer
import scala.collection.mutable.HashSet

import pink.cozydev.protosearch.codecs.IndexCodecs
import pink.cozydev.protosearch.internal.TermDictionary

sealed abstract class FrequencyIndex private (
    val termDict: TermDictionary,
    private val tfData: Array[Array[Int]],
    val numDocs: Int,
) extends Index {

  val numTerms = termDict.numTerms
  lazy val numData = tfData.map(_.size).sum / 2

  override def toString(): String = s"TermIndexArray($numTerms terms, $numData term-doc pairs)"

  def docCount(term: String): Int = {
    val idx = termDict.termIndex(term)
    if (idx < 0) 0
    else tfData(idx).size / 2
  }

  def docsWithTerm(term: String): Iterator[Int] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) Nil
    else evenElems(tfData(idx))
  }.iterator

  /** For every term between left and right, get the docs using those terms. */
  def docsForRange(left: String, right: String): Iterator[Int] = {
    val bldr = HashSet.empty[Int]
    Range(termDict.termIndexWhere(left), termDict.termIndexWhere(right))
      .foreach(i => bldr ++= evenElems(tfData(i)))
    bldr.iterator
  }

  /** For every term starting with prefix, get the docs using those terms. */
  def docsForPrefix(prefix: String): Iterator[Int] = {
    val terms = termDict.indicesForPrefix(prefix)
    if (terms.size == 0) Iterator.empty
    else {
      val bldr = HashSet.empty[Int]
      terms.foreach(i => bldr ++= evenElems(tfData(i)))
      bldr.iterator
    }
  }

  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Double)] =
    if (docs.size == 0) Nil
    else {
      val idx = termDict.termIndex(term)
      if (idx == -1) Nil
      else {
        val arr = tfData(idx)
        val idf: Double = 2.0 / arr.size.toDouble
        val bldr = ListBuffer.newBuilder[(Int, Double)]
        bldr.sizeHint(docs.size)
        docs.foreach { docId =>
          val i = indexForDocId(arr, docId)
          if (i >= 0) {
            val tf = Math.log(1.0 + arr(i + 1))
            val tfidf: Double = tf * idf
            // println(s"term($term) doc($docId) tf: $tf, idf: $idf, tfidf: $tfidf")
            bldr += (docId -> tfidf)
          }
        }
        bldr.result().sortBy(-_._2).toList
      }
    }

  private def evenElems(arr: Array[Int]): List[Int] = {
    require(
      arr.size >= 2 && arr.size % 2 == 0,
      "evenElems expects even sized arrays of 2 or greater",
    )
    if (arr.size == 2) arr(0) :: Nil
    else {
      var i = 0
      val bldr = ListBuffer.newBuilder[Int]
      bldr.sizeHint(arr.size / 2)
      while (i < arr.length) {
        bldr += arr(i)
        i += 2
      }
      bldr.result().toList
    }
  }

  private def indexForDocId(arr: Array[Int], id: Int): Int = {
    var i = 0
    while (i < arr.length) {
      if (arr(i) == id) return i
      i += 2
    }
    -1
  }

}
object FrequencyIndex {
  import scala.collection.mutable.{TreeMap => MMap}
  import scodec.{Codec, codecs}

  // don't want to take in Stream[F, Stream[F, A]] because we should really be taking in
  // a Stream[F, A] with evidence of Indexable[A]
  def apply(docs: List[List[String]]): FrequencyIndex = {
    val m = new MMap[String, List[Int]].empty
    var docId = 0
    val docLen = docs.length
    docs.foreach { doc =>
      doc.foreach { term =>
        var s = m.getOrElseUpdate(term, List.empty[Int])
        if (s.isEmpty) {
          // println(s"doc($docId), term($term), init freq = 1")
          // s.prepend(1).prepend(docId)
          s = docId :: 1 :: Nil
        } else {
          val lastDoc = s.head
          val freq = s.tail.head
          if (lastDoc == docId) {
            //   println(s"doc($docId), term($term), INCREMENT newFreq=${freq + 1}")
            // s.prepend(freq + 1).prepend(docId)
            s = docId :: (freq + 1) :: s.tail.tail
          } else {
            //   println(s"doc($docId), term($term) new doc! init freq = 1")
            // s.prepend(freq).prepend(lastDoc).prepend(1).prepend(docId)
            s = docId :: 1 :: s
          }
        }
        m += ((term, s))
      }
      docId += 1
    }
    val keys = ArrayBuilder.make[String]
    val values = ArrayBuilder.make[Array[Int]]
    val size = m.size
    keys.sizeHint(size)
    values.sizeHint(size)
    m.foreach { case (k, v) =>
      keys += k
      values += v.toArray
    }
    new FrequencyIndex(new TermDictionary(keys.result()), values.result(), docLen) {}
  }

  val codec: Codec[FrequencyIndex] = {
    val terms = TermDictionary.codec
    val postings = IndexCodecs.postings
    val numDocs = codecs.vint.withContext("numDocs")

    (numDocs :: postings :: terms)
      .as[(Int, Array[Array[Int]], TermDictionary)]
      .xmap(
        { case (numDocs, tfData, terms) => new FrequencyIndex(terms, tfData, numDocs) {} },
        ti => (ti.numDocs, ti.tfData, ti.termDict),
      )
  }
}
