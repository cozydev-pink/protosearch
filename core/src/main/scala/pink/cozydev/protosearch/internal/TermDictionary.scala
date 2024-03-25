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

import scala.annotation.tailrec
import scala.util.matching.Regex

final class TermDictionary(
    private val termDict: Array[String]
) {

  val numTerms = termDict.length

  override def toString(): String = s"TermDictionary($numTerms terms)"

  /** Find where the term would fit in the term list. */
  def termIndexWhere(term: String): Int = {
    val idx = termDict.indexWhere(_ >= term)
    if (idx == -1) termDict.length else idx
  }

  /** Get the list of terms between left and right. */
  def termsForRange(left: String, right: String): List[String] = {
    val li = termIndexWhere(left)
    val ri = termIndexWhere(right)
    termDict.slice(li, ri).toList
  }

  /** Get the list of terms starting with prefix . */
  def termsForPrefix(prefix: String): List[String] = {
    val bldr = List.newBuilder[String]
    indicesForPrefix(prefix).foreach(i => bldr += termDict(i))
    bldr.result()
  }

  /** Get the list of terms matching the regex. */
  def termsForRegex(regex: Regex): List[String] =
    termDict.filter(regex.findFirstIn(_).isDefined).toList

  /** Get the list of terms starting with prefix . */
  def indicesForPrefix(prefix: String): Array[Int] = {
    var i = termIndexWhere(prefix)
    if (i < termDict.length && termDict(i).startsWith(prefix)) {
      val bldr = Array.newBuilder[Int]
      while (i < termDict.size)
        if (termDict(i).startsWith(prefix)) {
          bldr += i
          i += 1
        } else return bldr.result()
      bldr.result()
    } else Array.empty[Int]
  }

  def termIndex(term: String): Int =
    binarySearch(term, 0, numTerms)

  @tailrec
  private def binarySearch(elem: String, from: Int, to: Int): Int =
    if (to <= from) -1 // term doesn't exist, prefix search should start around here
    else {
      val idx = from + (to - from - 1) / 2
      math.signum(elem.compareTo(termDict(idx))) match {
        case -1 => binarySearch(elem, from, idx)
        case 1 => binarySearch(elem, idx + 1, to)
        case _ => idx
      }
    }
}
object TermDictionary {
  import scodec.Codec
  import pink.cozydev.protosearch.codecs.IndexCodecs

  val codec: Codec[TermDictionary] =
    IndexCodecs.termList.xmap(
      { case terms => new TermDictionary(terms) },
      td => td.termDict,
    )
}
