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

object NextPositionToMatch {

  def indexOfMax(positions: Array[Int]): Int = {
    var i, maxI = 0
    while (i < positions.size) {
      maxI = if (positions(i) > positions(maxI)) i else maxI
      i += 1
    }
    maxI
  }

  def nextNotInOrderWithLargest(positions: Array[Int]): Int = {
    val maxI = indexOfMax(positions)
    // walk left until we find a position not in order or the beginning
    var i = maxI - 1
    var leftI = -1
    var ldiff = -1
    while (i >= 0 && leftI == -1) {
      val left = positions(i)
      val prev = positions(i + 1)
      ldiff = prev - left
      if (ldiff != 1)
        leftI = i
      else
        i -= 1
    }

    // walk right until we find a position not in order or the end
    i = maxI + 1
    var rightI = -1
    var rdiff = -1
    while (i < positions.size && rightI == -1) {
      val right = positions(i)
      val prev = positions(i - 1)
      rdiff = right - prev
      if (rdiff != 1)
        rightI = i
      else
        i += 1
    }

    // return the index of the position closest to being in order
    if (leftI == -1 && rightI == -1) -1 // all positions are in match
    else if (leftI == -1) rightI
    else if (rightI == -1) leftI
    else if (ldiff > rdiff) rightI
    else leftI
  }
}
