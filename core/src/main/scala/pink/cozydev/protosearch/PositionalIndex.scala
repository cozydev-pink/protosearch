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
    private val termDict: TermDictionary,
    private val tfData: Array[PositionalPostingsList],
    val numDocs: Int,
) {
  val numTerms = termDict.numTerms

  def docsWithTermSet(term: String): Set[Int] = {
    val idx = termDict.termIndex(term)
    if (idx < 0) Set.empty
    else {
      tfData(idx).docs.toSet
    }
  }

  /** For every term starting with prefix, get the docs using those terms. */
  def docsForPrefix(prefix: String): Set[Int] = {
    val terms = termDict.indicesForPrefix(prefix)
    if (terms.size == 0) Set.empty
    else {
      val bldr = HashSet.empty[Int]
      terms.foreach(i => bldr ++= tfData(i).docs)
      bldr.toSet
    }
  }

  /** For every term between left and right, get the docs using those terms. */
  def docsForRange(left: String, right: String): Set[Int] = {
    val bldr = HashSet.empty[Int]
    Range(termDict.termIndexWhere(left), termDict.termIndexWhere(right))
      .foreach(i => bldr ++= tfData(i).docs)
    bldr.toSet
  }

}
object PositionalIndex {
  import scala.collection.mutable.{TreeMap => MMap}

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

}
