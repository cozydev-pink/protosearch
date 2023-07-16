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

package pink.cozydev.protosearch.analysis

// Hopefully temporary, this should probably live in textmogrify
sealed class Analyzer private (
    tokenizer: (String) => List[String],
    lowerCase: Boolean,
    stopWords: Set[String],
) {

  def copy(
      tokenizer: (String => List[String]) = tokenizer,
      lowerCase: Boolean = lowerCase,
      stopWords: Set[String] = stopWords,
  ): Analyzer =
    new Analyzer(tokenizer, lowerCase, stopWords)

  def withLowerCasing: Analyzer =
    copy(lowerCase = true)

  def withTokenizer(tk: (String) => List[String]): Analyzer =
    copy(tokenizer = tk)

  def tokenize(s: String): List[String] =
    if (lowerCase)
      tokenizer(s.toLowerCase())
    else
      tokenizer(s)
}
object Analyzer {
  def default: Analyzer =
    new Analyzer(
      TokenStream.tokenizeSpaceL,
      lowerCase = false,
      stopWords = Set.empty,
    ) {}
}
