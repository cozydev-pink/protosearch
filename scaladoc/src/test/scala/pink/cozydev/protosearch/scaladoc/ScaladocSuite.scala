package pink.cozydev.protosearch.scaladoc
import munit.FunSuite

class ScaladocSuite extends FunSuite {

  val source = """
  /** cozydev-pink/protosearch/blob/main/core/src/main/scala/pink/cozydev/protosearch/Field.scala **/
  /** This is a Scaladoc comment
      * This is a description for the object Main.
      */
    object Main {
          /** This is a class representing a person
      * @param name The name of the person
      * @param age The age of the person
      */

      /** 
        * This function sums two integers.
        * @param a The first parameter
        * @param b The second parameter
        * @tparam T The type parameter
        */
      @deprecated("Use add instead", "1.0")
      def sum[T](a: Int, b: Int=1): Int = a + b

         /** 
        * This function greets the user.
        * @param name The name parameter
        */
      @throws(classOf[IllegalArgumentException])  
      def greet(name: String="Charles"): Unit = println(s"Hello, $name!")

      /** 
        * This function subtracts two integers.
        * @param c The first parameter to subtract
        * @param d The second parameter to subtract
        * @tparam T The type parameters
        */
        implicit val myvalue:Int=3
      def subtraction[T]( c: Int=21)(implicit d: Int): Int = c - d
       /** 
        * This function subtracts two integers.
        * @tparam A The type parameters
        * @tparam B The type parameters
        */
    }
    """
  test("Searcher should return correct info for query sum") {
    val result = ScaladocSearcher.searchScaladoc(source,List("sum"))

     val actual = result.head

     val expected = ScaladocInfo("sum",
      "This function sums two integers.",
      List("""@deprecated("Use add instead", "1.0")"""),
      List(),
      List("T: The type parameter","a: The first parameter: Int", "b: The second parameter: Int"),
      "Int",
      17,
      18
     )

    assertEquals(actual,expected)
  }

  test("Searcher should return correct info for query greet") {
    val result = ScaladocSearcher.searchScaladoc(source,List("greet"))

     val actual = result.head

     val expected = ScaladocInfo("greet",
      "This function greets the user.",
      List("@throws(classOf[IllegalArgumentException])"),
      List(),
      List("name: The name parameter: String"),
      "Unit",
      24,
      25
     )

    assertEquals(actual,expected)
  }

  test("Searcher should return correct info for query subtraction") {
    
    val result = ScaladocSearcher.searchScaladoc(source,List("subtraction"))

     val actual = result.head

     val expected = ScaladocInfo("subtraction",
      "This function subtracts two integers.",
      List(),
      List(),
      List("T: The type parameters","c: The first parameter to subtract: Int","d: The second parameter to subtract: Int"),
      "Int",
      34,
      34
     )

    assertEquals(actual,expected)
  }
}
