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
import pink.cozydev.protosearch.internal.QueryIterator
import pink.cozydev.protosearch.internal.OrQueryIterator
import pink.cozydev.protosearch.internal.ConstantScoreQueryIterator
import java.util.regex.Pattern

sealed abstract class FrequencyIndex private (
    val termDict: TermDictionary,
    private val tfData: Array[FrequencyPostingsList],
    val numDocs: Int,
) extends Index {

  val numTerms = termDict.numTerms

  override def toString(): String = s"FrequencyIndex($numTerms terms, $numDocs docs)"

  def docCount(term: String): Int = {
    val idx = termDict.termIndex(term)
    if (idx < 0) 0
    else tfData(idx).docs.size
  }

  def postingForTerm(term: String): Option[FrequencyPostingsList] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) None
    else {
      Some(tfData(idx))
    }
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

  def docsWithTermIter(term: String): QueryIterator = {
    val idx = termDict.termIndex(term)
    if (idx < 0) QueryIterator.empty
    else tfData(idx).queryIterator()
  }

  def docsForPrefixIter(prefix: String): QueryIterator = {
    val terms = termDict.indicesForPrefix(prefix)
    if (terms.size == 0) QueryIterator.empty
    else {
      val arr = new Array[QueryIterator](terms.size)
      var i = 0
      terms.foreach { idx =>
        arr(i) = tfData(idx).queryIterator()
        i += 1
      }
      new ConstantScoreQueryIterator(OrQueryIterator(arr, 1), 1.0f)
    }
  }

  def docsForRangeIter(left: String, right: String): QueryIterator = {
    // TODO Should check termIndex values for -1
    val range = Range(termDict.termIndexWhere(left), termDict.termIndexWhere(right))
    val arr = new Array[QueryIterator](range.size)
    var i = 0
    range.foreach { idx =>
      arr(i) = tfData(idx).queryIterator()
      i += 1
    }
    new ConstantScoreQueryIterator(OrQueryIterator(arr, 1), 1.0f)
  }

  def docsForRegexIter(pattern: Pattern): QueryIterator = {
    val terms = termDict.indicesForRegex(pattern)
    if (terms.size == 0) QueryIterator.empty
    else {
      val arr = new Array[QueryIterator](terms.size)
      var i = 0
      terms.foreach { idx =>
        arr(i) = tfData(idx).queryIterator()
        i += 1
      }
      new ConstantScoreQueryIterator(OrQueryIterator(arr, 1), 1.0f)
    }
  }

  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Float)] =
    if (docs.size == 0) Nil
    else {
      val idx = termDict.termIndex(term)
      if (idx == -1) Nil
      else {
        val posting = tfData(idx)
        val idf: Float = 2.0f / posting.docs.size.toFloat
        val bldr = ListBuffer.newBuilder[(Int, Float)]
        bldr.sizeHint(docs.size)
        docs.foreach { docId =>
          val freq = posting.frequencyForDocID(docId)
          if (freq != -1) {
            val tf: Float = Math.log(1.0 + freq).toFloat
            val tfidf: Float = tf * idf
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
    var docId = 1 // docIds start at 1 so iters can start at 0
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
