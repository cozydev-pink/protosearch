import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer


case class Book(author: String, title: String)

case class ScaladocInfo(description: String, params: List[String], tparams: List[String])

object Scalaparser {

  def parseAndExtractInfo(source: String): Map[String, ScaladocInfo] = {
    val parsed: Source = source.parse[Source].get

    // Extract comments and their positions
    val comments = parsed.tokens.collect {
      case comment: Comment if comment.syntax.startsWith("/**") =>
        (comment.pos.start, ScaladocParser.parseScaladoc(comment).getOrElse(Nil))
    }.toMap

    // Traverse the AST to find functions and their associated comments
    val functions = parsed.collect {
      case defn @ Defn.Def(_, name, _, _, _, _) =>
        // Debugging: Print the position of the function
        // println(s"Function: ${name.value} at position ${defn.pos.start}")

        // Find the closest preceding comment
        val commentTokens = comments
          .filter { case (start, _) => start < defn.pos.start }
          .toSeq
          .sortBy(_._1)
          .lastOption
          .map(_._2)
          .getOrElse(Nil)

        val description = commentTokens.collect {
          case DocToken(Description, _, body) => body.getOrElse("")
        }.mkString(" ")

        val params = commentTokens.collect {
          case DocToken(Param, Some(name), desc) => s"@$name: ${desc.getOrElse("")}"
        }

        val tparams = commentTokens.collect {
          case DocToken(TypeParam, Some(name), desc) => s"@tparam $name: ${desc.getOrElse("")}"
        }

        name.value -> ScaladocInfo(description, params, tparams)
    }.toMap

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

  val scaladocInfoList = Scalaparser.parseAndExtractInfo(source)

  // scaladocInfoList.foreach { case (name, info) =>
  //   println(s"Function: $name")
  //   println(s"  Description: ${info.description}")
  //   println(s"  Params: ${info.params.mkString(", ")}")
  //   println(s"  Type Params: ${info.tparams.mkString(", ")}")
  // }

  val analyzer = Analyzer.default.withLowerCasing
  val indexBldr = IndexBuilder.of[(String,ScaladocInfo)](
    (Field("functionName", analyzer, stored=true, indexed=true, positions=true), _._1),
    (Field("description", analyzer, stored=true, indexed=true, positions=true), _._2.description),
      (Field("params", analyzer, stored=true, indexed=true, positions=true), _._2.params.mkString(", ")),
      (Field("tparams", analyzer, stored=true, indexed=true, positions=true), _._2.tparams.mkString(", "))
  )

  val index = indexBldr.fromList(scaladocInfoList.toList)

  val qAnalyzer = index.queryAnalyzer

  def search(q: String): List[(String, ScaladocInfo)] = {
    val searchResults = index.search(q)
    searchResults.fold(_ => Nil, hits => hits.map(h => scaladocInfoList.toList(h.id)))
  }

  val queries = List("sum", "greet", "subtract")
    queries.foreach { query =>
      val results = search(query)
      println(s"Search results for query '$query':")
      results.foreach { case (name, info) =>
        println(s"Function Name: $name")
        println(s"  Description: ${info.description}")
        println(s"  Params: ${info.params.mkString(", ")}")
        println(s"  Type Params: ${info.tparams.mkString(", ")}")
      }
    }

}
