import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer
import scala.util.matching.Regex


//case class Book(author: String, title: String)

case class ScaladocInfo( 
  name: String,
  params: List[String],
  tparams: List[String],
  description: String,
  annotations: List[String],
  hyperlinks: List[String],
  optionalParams: List[String],
  implicitParams: List[String],
  originalDefn: Defn.Def)

object Scalaparser {

  def parseAndExtractInfo(source: String): List[ScaladocInfo] = {
    val parsed: Source = source.parse[Source].get

    val urlRegex: Regex = """https?://[^\s]+""".r

    // Extract comments and their positions
    val comments = parsed.tokens.collect {
      case comment: Comment if comment.syntax.startsWith("/**") =>
        (comment.pos.start, ScaladocParser.parseScaladoc(comment).getOrElse(Nil))
    }.toMap

    val functions = parsed.collect {
      case defn @ Defn.Def(mods, name, tparams, paramss, _, _) =>
      val (commentTokens, rawComment): (List[DocToken], String) = comments
      .filter { case (start, _) => start < defn.pos.start }
      .toList // convert to List, as maxBy works on Iterable
      .sortBy(_._1) // sort by the start position
      .reverse.headOption // get the last one, which will be the max
      .map { case (_, tokens) =>
        val raw = tokens.collect {
          case DocToken(_, _, Some(body)) => body
        }.mkString(" ")
        (tokens, raw)
      }
      .getOrElse((Nil, ""))

      val description = commentTokens.collect {
        case DocToken(Description, _, Some(body)) => body
      }.mkString(" ")

      val paramsComm  = commentTokens.collect {
        case DocToken(Param, Some(name), Some(desc)) => s"$name: $desc"
      }

      val params = paramss.flatten.map { param =>
        s"${paramsComm.find(_.startsWith(""+param.name.value)).getOrElse(param.name.value)}: ${param.decltpe.map(_.toString).getOrElse("Unknown Type")} " 
      }

      val typeParamsComm= commentTokens.collect {
        case DocToken(TypeParam, Some(name), Some(desc)) => s"@tparam $name: $desc"
      }

      val typeParams = tparams.zipWithIndex.map { case (tparam, index)    =>
        val value = typeParamsComm.lift(index).getOrElse(tparam.name.value)
        s"${value.replace("@tparam", "")}"
      }
      
      val annotations = defn.mods.collect {
        case mod: Mod.Annot => mod.toString
      }

      val hyperlinks = urlRegex.findAllMatchIn(rawComment).map(_.matched).toList

      val optionalParams = paramss.flatten.collect {
        case param: Term.Param if param.default.isDefined => param.name.value
      }

      val implicitParams = paramss.collect {
        case params if params.exists(_.mods.exists(_.is[Mod.Implicit])) =>
          params.collect {
            case param: Term.Param => param.name.value
          }
      }.flatten

      ScaladocInfo(
        name.value,
        params,
        typeParams,
        description , 
        annotations,
        hyperlinks , 
        optionalParams,
        implicitParams,
        originalDefn = defn
      )
    }

    functions
  }
}

object ParserTest extends App {
  println("Welcome to Standard Scaladoc Parser")

  val source = """
    /** This is a Scaladoc comment
      * This is a description for the object Main.
      */
    object Main {
      /** 
        * This function sums two integers.
        * @param a The first parameter
        * @param b The second parameter
        * @tparam T The type parameter
        */
      def sum[T](a: Int, b: Int): Int = a + b

      /** 
        * This function greets the user.
        * @param name The name parameter
        */
      def greet(name: String): Unit = println(s"Hello, $name!")

      /** 
        * This function subtracts two integers.
        * @param c The first parameter to subtract
        * @param d The second parameter to subtract
        * @tparam T The type parameters
        */
      def subtract[T](c: Int, d: Int): Int = c - d
    }
  """
  //  val scaladocInfoList = Scalaparser.parseAndExtractInfo(source)

  // scaladocInfoList.foreach { case (name, info) =>
  //   println(s"Function: $name")
  //   println(s"  Description: ${info.description}")
  //   println(s"  Params: ${info.params.mkString(", ")}")
  //   println(s"  Type Params: ${info.tparams.mkString(", ")}")
  // }

  val scaladocInfoList = Scalaparser.parseAndExtractInfo(source)

  scaladocInfoList.foreach { info =>
    println(s"Function: ${info.name}")
    println(s"  Description: ${info.description}")
    println(s"  Params: ${info.params.mkString(", ")}")
    println(s"  Type Params: ${info.tparams.mkString(", ")}")
    println(s"  Annotations: ${info.annotations.mkString(", ")}")
    println(s"  Hyperlinks: ${info.hyperlinks.mkString(", ")}")
    println(s"  Optional Params: ${info.optionalParams.mkString(", ")}")
    println(s"  Implicit Params: ${info.implicitParams.mkString(", ")}")
  }

  val analyzer = Analyzer.default.withLowerCasing
  val indexBldr = IndexBuilder.of[ScaladocInfo](
    (Field("functionName", analyzer, stored=true, indexed=true, positions=true),_.name),
    (Field("description", analyzer, stored=true, indexed=true, positions=true), _.description),
    (Field("params", analyzer, stored=true, indexed=true, positions=true), _.params.mkString(", ")),
    (Field("tparams", analyzer, stored=true, indexed=true, positions=true), _.tparams.mkString(", ")),
    (Field("Annotations", analyzer, stored=true, indexed=true, positions=true), _.annotations.mkString(", ")),
    (Field("Hyperlinks", analyzer, stored=true, indexed=true, positions=true), _.hyperlinks.mkString(", ")),
    (Field("Optional Params", analyzer, stored=true, indexed=true, positions=true), _.optionalParams.mkString(", ")),
    (Field("Implicit Params", analyzer, stored=true, indexed=true, positions=true), _.implicitParams.mkString(", "))
  )

  val index = indexBldr.fromList(scaladocInfoList)

  val qAnalyzer = index.queryAnalyzer

  def search(q: String): List[ScaladocInfo] = {
    val searchResults = index.search(q)
    searchResults.fold(_ => Nil, hits => hits.map(h => scaladocInfoList.toList(h.id)))
  }

  val queries = List("sum", "greet", "subtract")
    queries.foreach { query =>
      val results = search(query)
      println(s"Search results for query '$query':")
      results.foreach { info =>
        // println(s"Function Name: $name")
        // println(s"  Description: ${info.description}")
        // println(s"  Params: ${info.params.mkString(", ")}")
        // println(s"  Type Params: ${info.tparams.mkString(", ")}")
         println(s"Function: ${info.name}")
        println(s"  Description: ${info.description}")
        println(s"  Params: ${info.params.mkString(", ")}")
        println(s"  Type Params: ${info.tparams.mkString(", ")}")
        println(s"  Annotations: ${info.annotations.mkString(", ")}")
        println(s"  Hyperlinks: ${info.hyperlinks.mkString(", ")}")
        println(s"  Optional Params: ${info.optionalParams.mkString(", ")}")
        println(s"  Implicit Params: ${info.implicitParams.mkString(", ")}")
      }
    }

}
