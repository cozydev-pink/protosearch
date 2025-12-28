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
  private val storedFields = multiIndex.schema.storedFields

  def search(request: SearchRequest): SearchResult = {
    val parseQ = queryAnalyzer
      .parse(request.query)
      .map(q => if (request.lastTermPrefix) q.mapLastTerm(LastTermRewrite.termToPrefix) else q)
    val getHits: Either[String, List[Hit]] = parseQ.flatMap(q =>
      hitBuilder(request).flatMap(hitBldr =>
        indexSearcher
          .search(q)
          .flatMap(ds => scorer.score(q, ds, request.size))
          .map(hits => hits.map(hit => hitBldr(hit)))
      )
    )
    getHits match {
      case Left(err) => SearchFailure(err)
      case Right(hits) => SearchSuccess(hits)
    }
  }

  private def hitBuilder(request: SearchRequest): Either[String, ((Int, Float)) => Hit] = {
    // Ensure all requested fields are present in the index and stored fields
    val errFs: List[String] =
      request.resultFields.map(fs => fs.filterNot(storedFields)).getOrElse(Nil)
    val errHLs: List[String] =
      request.highlightFields.map(hs => hs.filterNot(storedFields)).getOrElse(Nil)
    if (errFs.nonEmpty || errHLs.nonEmpty) {
      val fStr =
        if (errFs.nonEmpty) s"Fields not stored in index: '${errFs.mkString(", ")}'." else ""
      val hStr =
        if (errHLs.nonEmpty) s"Highlights not stored in index: '${errHLs.mkString(", ")}'." else ""
      Left(List(fStr, hStr).filter(_.nonEmpty).mkString(" "))
    } else {
      // All requested fields are present
      val fSet: Set[String] = request.resultFields.fold(storedFields)(fs => fs.toSet)
      val hSet: Set[String] = request.highlightFields.fold(storedFields)(hs => hs.toSet)
      Right { case (docId, score) =>
        val fieldBldr = Map.newBuilder[String, String]
        val highlightBldr = Map.newBuilder[String, String]
        fSet.union(hSet).foreach { (f: String) =>
          val field = multiIndex.fields.get(f)
          field.foreach { arr =>
            val value = arr(docId - 1)
            if (fSet.contains(f)) fieldBldr += f -> value
            if (hSet.contains(f)) highlightBldr += f -> highlighter.highlight(value, request.query)
          }
        }
        Hit(docId, score, fieldBldr.result(), highlightBldr.result())
      }
    }
  }
}
object Searcher {
  private val defaultHighlighter =
    FirstMatchHighlighter(FragmentFormatter(100, "<b>", "</b>"))

  def default(index: MultiIndex): Searcher =
    Searcher(index, defaultHighlighter)
}
