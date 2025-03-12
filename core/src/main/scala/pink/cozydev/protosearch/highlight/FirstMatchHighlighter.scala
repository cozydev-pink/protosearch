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

package pink.cozydev.protosearch.highlight

case class FirstMatchHighlighter(
    formatter: FragmentFormatter
) {

  val lookBackWindowSize: Int = formatter.maxSize / 2

  def trim(str: String): String = {
    val trimmed = str.trim()
    if (trimmed.size > formatter.maxSize)
      trimmed.take(formatter.maxSize) + "..."
    else trimmed
  }

  def highlight(str: String, queryStr: String): String = {
    // lowercase both in place, to find case-insensitive matches
    val offset = str.toLowerCase().indexOf(queryStr.toLowerCase())
    if (offset == -1)
      // 'queryStr' does not appear in 'str'
      trim(str)
    else {
      val start = Math.max(0, offset - lookBackWindowSize)
      if (start == 0 || str.size < formatter.maxSize) {
        // First match 'offset' is within first 'lookBackWindowSize' characters,
        // or the whole 'str' is within formatter max, no slicing necessary.
        val fStr = formatter.format(str, List(offset, queryStr.size))
        trim(fStr)
      } else {
        // First match 'offset' not within first 'lookBackWindowSize' characters,
        // or 'str' is too big, need to find a nearby starting place from 'start'.
        val nearby = str.indexWhere(c => " \n\t.".contains(c), start)
        val slice = str.drop(nearby)
        val newOffset = offset - nearby
        val fStr = formatter.format(slice, List(newOffset, queryStr.size))
        trim(fStr)
      }
    }
  }

}
