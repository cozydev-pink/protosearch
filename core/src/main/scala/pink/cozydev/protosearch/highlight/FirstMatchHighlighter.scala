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

/** Highlights the first case-insensitive occurrence of a query string within text,
  * trimming long text to a window around the match.
  *
  * If no match is found, the text is returned trimmed to `maxSize`. When the match
  * occurs far into the text, a nearby word boundary is used as the starting point
  * for the returned fragment.
  *
  * @param formatter the formatter used to wrap matches with tags
  * @param maxSize maximum number of content characters (excluding tags) in the result
  */
case class FirstMatchHighlighter(
    formatter: FragmentFormatter,
    maxSize: Int,
) {

  /** How far back from a match to start the window. */
  val lookBackWindowSize: Int = maxSize / 2

  // trim and add '...' if needed
  private def trim(str: String): String = {
    val trimmed = str.trim()
    val maxWithTags = maxSize + formatter.tagSize
    if (trimmed.size > maxWithTags)
      trimmed.take(maxWithTags) + "..."
    else trimmed
  }

  /** Returns `str` with the first case-insensitive occurrence of `queryStr` wrapped in
    * formatter tags, trimmed to a window of `maxSize` content characters.
    * If `queryStr` is not found, returns the trimmed text without highlighting.
    */
  def highlight(str: String, queryStr: String): String = {
    // lowercase both in place, to find case-insensitive matches
    val normalizedQ = queryStr.trim().toLowerCase()
    val offset = str.toLowerCase().indexOf(normalizedQ)
    if (offset == -1)
      // 'normalizedQ' does not appear in 'str'
      trim(str)
    else {
      val start = Math.max(0, offset - lookBackWindowSize)
      if (start == 0 || str.size < maxSize) {
        // First match 'offset' is within first 'lookBackWindowSize' characters,
        // or the whole 'str' is within formatter max, no slicing necessary.
        val fStr = formatter.format(str, List(offset, normalizedQ.size))
        trim(fStr)
      } else {
        // First match 'offset' not within first 'lookBackWindowSize' characters,
        // or 'str' is too big, need to find a nearby starting place from 'start'.
        val nearbyOrStart = str.indexWhere(c => " \n\t.".contains(c), start) match {
          case -1 => start // no boundary nearby, use start
          case i if i > offset => start // boundary past match, just use start
          case i => i
        }
        val slice = str.drop(nearbyOrStart)
        val newOffset = offset - nearbyOrStart
        val fStr = formatter.format(slice, List(newOffset, normalizedQ.size))
        trim(fStr)
      }
    }
  }

}
