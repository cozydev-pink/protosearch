import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer

object ScaladocIndexer{
  val source = """ Some Source """
  val scaladocInfoList = ScaladocParser.parseAndExtractInfo(source)

  // scaladocInfoList.foreach { info =>
  //     println(s"Function: ${info.name}")
  //     println(s"  Description: ${info.description}")
  //     println(s"  Params: ${info.params.mkString(", ")}")
  //     println(s"  Annotations: ${info.annotations.mkString(", ")}")
  //     println(s"  Hyperlinks: ${info.hyperlinks.mkString(", ")}")
  //     println(s"  Startline: ${info.startLine}")
  //     println(s"  Endline: ${info.endLine}")
  //     println(s"  Return type: ${info.returnType}")
  // }

  val analyzer = Analyzer.default.withLowerCasing
  val indexBldr = IndexBuilder.of[ScaladocInfo](
      (Field("functionName", analyzer, stored=true, indexed=true, positions=true),_.name),
      (Field("description", analyzer, stored=true, indexed=true, positions=true), _.description),
      (Field("params", analyzer, stored=true, indexed=true, positions=true), _.params.mkString(", ")),
      (Field("annotations", analyzer, stored=true, indexed=true, positions=true), _.annotations.mkString(", ")),
      (Field("startLine", analyzer, stored=true, indexed=true, positions=true), _.startLine.toString()),
      (Field("endLine", analyzer, stored=true, indexed=true, positions=true), _.endLine.toString()),
      (Field("hyperlinks", analyzer, stored=true, indexed=true, positions=true), _.hyperlinks.mkString(", ")),
      (Field("returnType", analyzer, stored=true, indexed=true, positions=true), _.returnType)
  )

  val index = indexBldr.fromList(scaladocInfoList)

  val qAnalyzer = index.queryAnalyzer

  def search(q: String): List[ScaladocInfo] = {
      val searchResults = index.search(q)
      searchResults.fold(_ => Nil, hits => hits.map(h => scaladocInfoList.toList(h.id)))
  }

  // val queries = List("of")
  //     queries.foreach { query =>
  //     val results = search(query)
  //     println(s"Search results for query '$query':")
  //     results.foreach { info =>
  //         println(s"Function: ${info.name}")
  //         println(s"  Description: ${info.description}")
  //         println(s"  Params: ${info.params.mkString(", ")}")
  //         println(s"  Annotations: ${info.annotations.mkString(", ")}")
  //         println(s"  Hyperlinks: ${info.hyperlinks.mkString(", ")}")
  //         println(s"  Startline: ${info.startLine}")
  //         println(s"  Endline: ${info.endLine}")
  //         println(s"  Return type: ${info.returnType}")
  //     }
  // }
}