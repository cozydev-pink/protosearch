package pink.cozydev.protosearch

import pink.cozydev.protosearch.MultiIndex
import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.protosearch.analysis.QueryAnalyzer

/**  For a type `A`, a SearchSchema describes the fields of the document representation
  *  of `A`.
  */
case class SearchSchema[A] private (
    private val schema: Map[String, (A => String, Analyzer)]
) {
  def queryAnalyzer(defaultField: String): QueryAnalyzer = {
    val analyzers = schema.view.mapValues(_._2).toList
    QueryAnalyzer(defaultField, analyzers.head, analyzers.tail: _*)
  }

  def indexBldr(defaultField: String): List[A] => MultiIndex = {
    val ss = schema.toList
    MultiIndex(defaultField, ss.head, ss.tail: _*)
  }
}
object SearchSchema {
  def apply[A](
      head: (String, (A => String, Analyzer)),
      tail: (String, (A => String, Analyzer))*
  ): SearchSchema[A] =
    new SearchSchema[A]((head :: tail.toList).toMap)
}

object Example {
  case class Doc(title: String, author: String)

  val analyzer = Analyzer.default

  val s = SearchSchema[Doc](
    "title" -> (_.title, analyzer),
    "author" -> (_.author, analyzer),
  )
}
