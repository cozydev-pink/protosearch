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

import pink.cozydev.protosearch.internal.QueryIterator
import java.util.regex.Pattern

trait Index {
  def numDocs: Int
  def numTerms: Int

  // Query Iterator
  def docsWithTermIter(term: String): QueryIterator
  def docsForRangeIter(left: String, right: String): QueryIterator
  def docsForPrefixIter(prefix: String): QueryIterator
  def docsForRegexIter(prefix: Pattern): QueryIterator
}
