package pink.cozydev.protosearch

import pink.cozydev.protosearch.highlight.FirstMatchHighlighter
import pink.cozydev.protosearch.highlight.FragmentFormatter

final case class SearchInterpreter(
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
    // TODO push down request size
    val getDocs: Either[String, List[(Int, Double)]] =
      parseQ.flatMap(q =>
        indexSearcher
          .search(q)
          .flatMap(ds => scorer.score(q, ds))
      )

    val lstB = List.newBuilder[Hit]
    lstB.sizeHint(request.size)
    getDocs match {
      case Left(err) => SearchFailure(err)
      case Right(docs) =>
        docs.foreach { case (docId, score) =>
          val fieldBldr = Map.newBuilder[String, String]
          request.resultFields.foreach(f =>
            multiIndex.fields.get(f).foreach(arr => fieldBldr += f -> arr(docId))
          )
          val highlightBldr = Map.newBuilder[String, String]
          request.highlightFields.foreach { hf =>
            val field = multiIndex.fields.get(hf)
            field.foreach { arr =>
              val h = highlighter.highlight(arr(docId), request.query)
              highlightBldr += hf -> h
            }
          }

          // TODO Update Hit highlight to be Map
          lstB += Hit(docId, score, fieldBldr.result(), highlightBldr.result().head._2)
        }
        SearchSuccess(lstB.result())
    }
  }
}
object SearchInterpreter {
  private val defaultHighlighter =
    FirstMatchHighlighter(FragmentFormatter(100, "<b>", "</b>"))

  def default(index: MultiIndex): SearchInterpreter =
    SearchInterpreter(index, defaultHighlighter)
}
