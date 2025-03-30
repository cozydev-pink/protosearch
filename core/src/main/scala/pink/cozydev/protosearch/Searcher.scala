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

import pink.cozydev.protosearch.highlight.FirstMatchHighlighter
import pink.cozydev.protosearch.highlight.FragmentFormatter
import pink.cozydev.protosearch.internal.IndexSearcher

final case class Searcher(
    multiIndex: MultiIndex,
    highlighter: FirstMatchHighlighter,
) {
  private val indexSearcher = IndexSearcher(multiIndex, multiIndex.schema.defaultOR)
  private val scorer = Scorer(multiIndex, multiIndex.schema.defaultOR)
  private val queryAnalyzer = multiIndex.schema.queryAnalyzer(multiIndex.schema.defaultField)

  def search(request: SearchRequest): SearchResult = {
    val parseQ = queryAnalyzer
      .parse(request.query)
      .map(q => if (request.lastTermPrefix) q.mapLastTerm(LastTermRewrite.termToPrefix) else q)
    val getDocs: Either[String, List[(Int, Float)]] =
      parseQ.flatMap(q =>
        indexSearcher
          .search(q)
          .flatMap(ds => scorer.score(q, ds, request.size))
      )

    val lstB = List.newBuilder[Hit]
    lstB.sizeHint(request.size)
    getDocs match {
      case Left(err) => SearchFailure(err)
      case Right(docs) =>
        docs.foreach { case (docId, score) =>
          val docOffset = docId - 1 // Because docIds start at 1, not 0
          val fieldBldr = Map.newBuilder[String, String]
          request.resultFields.foreach(f =>
            multiIndex.fields.get(f).foreach(arr => fieldBldr += f -> arr(docOffset))
          )
          val highlightBldr = Map.newBuilder[String, String]
          request.highlightFields.foreach { hf =>
            val field = multiIndex.fields.get(hf)
            field.foreach { arr =>
              val h = highlighter.highlight(arr(docOffset), request.query)
              highlightBldr += hf -> h
            }
          }
          val highlights = highlightBldr.result()
          lstB += Hit(docId, score, fieldBldr.result(), highlights)
        }
        SearchSuccess(lstB.result())
    }
  }
}
object Searcher {
  private val defaultHighlighter =
    FirstMatchHighlighter(FragmentFormatter(100, "<b>", "</b>"))

  def default(index: MultiIndex): Searcher =
    Searcher(index, defaultHighlighter)
}
