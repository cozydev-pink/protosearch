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
import scala.collection.mutable.HashSet

import pink.cozydev.protosearch.internal.PositionalPostingsList
import pink.cozydev.protosearch.internal.PositionalPostingsBuilder
import pink.cozydev.protosearch.internal.TermDictionary

sealed abstract class PositionalIndex private (
    val termDict: TermDictionary,
    private val tfData: Array[PositionalPostingsList],
    val numDocs: Int,
) extends Index {
  val numTerms = termDict.numTerms

  def docCount(term: String): Int = {
    val idx = termDict.termIndex(term)
    if (idx < 0) 0
    else {
      tfData(idx).docs.size
    }
  }

  def postingForTerm(term: String): Option[PositionalPostingsList] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) None
    else {
      Some(tfData(idx))
    }
  }

  def docsWithTerm(term: String): Iterator[Int] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) Iterator.empty
    else {
      tfData(idx).docs
    }
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

  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Float)] =
    if (docs.size == 0) Nil
    else {
      val idx = termDict.termIndex(term)
      if (idx == -1) Nil
      else {
        val posting = tfData(idx)
        val idf: Float = 2.0f / posting.docs.size.toFloat
        val bldr = List.newBuilder[(Int, Float)]
        bldr.sizeHint(docs.size)
        docs.foreach { docId =>
          val freq = posting.frequencyForDocID(docId)
          if (freq != -1) {
            val tf = Math.log(1.0 + freq).toFloat
            val tfidf: Float = tf * idf
            bldr += (docId -> tfidf)
          }
        }
        bldr.result().sortBy(-_._2).toList
      }
    }
}
object PositionalIndex {
  import scala.collection.mutable.{TreeMap => MMap}
  import scodec.{Codec, codecs}
  import pink.cozydev.protosearch.codecs.IndexCodecs

  def apply(docs: Iterable[Iterable[String]]): PositionalIndex = {
    val termPostingsMap = new MMap[String, PositionalPostingsBuilder].empty
    var docId = 0
    docs.foreach { doc =>
      var position = 0
      doc.foreach { term =>
        termPostingsMap
          .getOrElseUpdate(term, new PositionalPostingsBuilder)
          .addTermPosition(docId, position)
        position += 1
      }
      docId += 1
    }
    val keys = ArrayBuilder.make[String]
    val values = ArrayBuilder.make[PositionalPostingsList]
    val size = termPostingsMap.size
    keys.sizeHint(size)
    values.sizeHint(size)
    termPostingsMap.foreach { case (k, v) =>
      keys += k
      values += v.toPositionalPostingsList
    }
    new PositionalIndex(new TermDictionary(keys.result()), values.result(), docId) {}
  }

  val codec: Codec[PositionalIndex] = {
    val terms = TermDictionary.codec
    val postings =
      IndexCodecs.arrayOfN(codecs.vint, PositionalPostingsList.codec).withContext("postings")
    val numDocs = codecs.vint.withContext("numDocs")

    (numDocs :: postings :: terms)
      .as[(Int, Array[PositionalPostingsList], TermDictionary)]
      .xmap(
        { case (numDocs, tfData, terms) => new PositionalIndex(terms, tfData, numDocs) {} },
        idx => (idx.numDocs, idx.tfData, idx.termDict),
      )
  }

}
