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
    val offset = str.indexOf(queryStr)
    if (offset == -1)
      trim(str)
    else {
      val start = Math.max(0, offset - lookBackWindowSize)
      val nearby = str.indexWhere(c => " \n\t.".contains(c), start)
      val slice = if (str.size < formatter.maxSize) str else str.drop(nearby)
      val newOffset = if (str.size < formatter.maxSize) offset else offset - nearby
      val fStr = formatter.format(slice, List(newOffset, queryStr.size))
      trim(fStr)
    }
  }

}
