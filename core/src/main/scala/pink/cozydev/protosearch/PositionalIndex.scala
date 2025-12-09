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

import pink.cozydev.protosearch.internal.PositionalPostingsList
import pink.cozydev.protosearch.internal.PositionalPostingsBuilder
import pink.cozydev.protosearch.internal.TermDictionary
import pink.cozydev.protosearch.internal.QueryIterator
import pink.cozydev.protosearch.internal.ConstantScoreQueryIterator
import pink.cozydev.protosearch.internal.OrQueryIterator
import java.util.regex.Pattern

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
}
object PositionalIndex {
  import scala.collection.mutable.{TreeMap => MMap}
  import scodec.{Codec, codecs}
  import pink.cozydev.protosearch.codecs.IndexCodecs

  def apply(docs: Iterable[Iterable[String]]): PositionalIndex = {
    val termPostingsMap = new MMap[String, PositionalPostingsBuilder].empty
    var docId = 1 // docIds start at 1 so iterators can start at 0
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
    new PositionalIndex(new TermDictionary(keys.result()), values.result(), docId - 1) {}
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
