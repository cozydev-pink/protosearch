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
import pink.cozydev.protosearch.internal.FrequencyPostingsList
import pink.cozydev.protosearch.internal.FrequencyPostingsBuilder

sealed abstract class FrequencyIndex private (
    val termDict: TermDictionary,
    private val tfData: Array[FrequencyPostingsList],
    val numDocs: Int,
) extends Index {

  val numTerms = termDict.numTerms

  override def toString(): String = s"TermIndexArray($numTerms terms, $numDocs docs)"

  def docCount(term: String): Int = {
    val idx = termDict.termIndex(term)
    if (idx < 0) 0
    else tfData(idx).docs.size
  }

  def docsWithTerm(term: String): Iterator[Int] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) Iterator.empty
    else tfData(idx).docs
  }

  /** For every term starting with prefix, get the docs using those terms. */
  def docsForPrefix(prefix: String): Iterator[Int] = {
    val terms = termDict.indicesForPrefix(prefix)
    if (terms.size == 0) Iterator.empty
    else {
      val bldr = HashSet.empty[Int]
      terms.foreach(i => bldr ++= tfData(i).docs)
      bldr.iterator
    }
  }

  /** For every term between left and right, get the docs using those terms. */
  def docsForRange(left: String, right: String): Iterator[Int] = {
    val bldr = HashSet.empty[Int]
    Range(termDict.termIndexWhere(left), termDict.termIndexWhere(right))
      .foreach(i => bldr ++= tfData(i).docs)
    bldr.iterator
  }

  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Double)] =
    if (docs.size == 0) Nil
    else {
      val idx = termDict.termIndex(term)
      if (idx == -1) Nil
      else {
        val posting = tfData(idx)
        val idf: Double = 2.0 / posting.docs.size.toDouble
        val bldr = ListBuffer.newBuilder[(Int, Double)]
        bldr.sizeHint(docs.size)
        docs.foreach { docId =>
          val freq = posting.frequencyForDocID(docId)
          if (freq != -1) {
            val tf = Math.log(1.0 + freq)
            val tfidf: Double = tf * idf
            // println(s"term($term) doc($docId) tf: $tf, idf: $idf, tfidf: $tfidf")
            bldr += (docId -> tfidf)
          }
        }
        bldr.result().sortBy(-_._2).toList
      }
    }
}
object FrequencyIndex {
  import scala.collection.mutable.{TreeMap => MMap}
  import scodec.{Codec, codecs}

  def apply(docs: Iterable[Iterable[String]]): FrequencyIndex = {
    val termPostingsMap = new MMap[String, FrequencyPostingsBuilder].empty
    var docId = 0
    docs.foreach { doc =>
      doc.foreach { term =>
        termPostingsMap
          .getOrElseUpdate(term, new FrequencyPostingsBuilder)
          .addTerm(docId)
      }
      docId += 1
    }
    val keys = ArrayBuilder.make[String]
    val values = ArrayBuilder.make[FrequencyPostingsList]
    val size = termPostingsMap.size
    keys.sizeHint(size)
    values.sizeHint(size)
    termPostingsMap.foreach { case (k, v) =>
      keys += k
      values += v.toFrequencyPostingsList
    }
    new FrequencyIndex(new TermDictionary(keys.result()), values.result(), docId) {}
  }

  val codec: Codec[FrequencyIndex] = {
    val terms = TermDictionary.codec
    val postings =
      IndexCodecs.arrayOfN(codecs.vint, FrequencyPostingsList.codec).withContext("postings")
    val numDocs = codecs.vint.withContext("numDocs")

    (numDocs :: postings :: terms)
      .as[(Int, Array[FrequencyPostingsList], TermDictionary)]
      .xmap(
        { case (numDocs, tfData, terms) => new FrequencyIndex(terms, tfData, numDocs) {} },
        ti => (ti.numDocs, ti.tfData, ti.termDict),
      )
  }
}
