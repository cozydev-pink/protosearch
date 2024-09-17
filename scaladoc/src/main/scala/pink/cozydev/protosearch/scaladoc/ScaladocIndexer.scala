package pink.cozydev.protosearch.scaladoc

import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer
import pink.cozydev.protosearch.MultiIndex

object ScaladocIndexer{

  def createScaladocIndex(scaladocInfoList: List[ScaladocInfo]): MultiIndex = {
    
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

    index
  }
}