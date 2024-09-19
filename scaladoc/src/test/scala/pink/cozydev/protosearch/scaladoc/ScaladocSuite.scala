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
  val parseTestSum = """
  object Main {
    /**
      * This function sums two integers.
      * @param a The first parameter
      * @param b The second parameter
      * @tparam T The type parameter
      */
    @deprecated("Use add instead", "1.0")
    def sum[T](a: Int, b: Int=1): Int = a + b
  }
  """
  val parseTestfromList = """
  package pink.cozydev.protosearch

import scala.collection.mutable.ListBuffer

/** An intermediate helper for iterating over documents and building an Index */
final case class IndexBuilder[A] private (
    fieldAndGetters: List[(Field, A => String)],
    defaultField: String,
) {
  val schema = Schema(fieldAndGetters.head._1, fieldAndGetters.tail.map(_._1))

  def fromList(docs: List[A]): MultiIndex = {
    val fields = fieldAndGetters.map(_._1)
    val buffers: Map[String, ListBuffer[List[String]]] =
      fields.map(k => (k.name, ListBuffer.empty[List[String]])).toMap
    val storage: Map[String, ListBuffer[String]] =
      fields.map(k => (k.name, ListBuffer.empty[String])).toMap

    docs.foreach { doc =>
      fieldAndGetters.foreach { case (field, getter) =>
        val fieldValue = getter(doc)
        storage(field.name) += fieldValue
        buffers(field.name) += field.analyzer.tokenize(fieldValue)
      }
    }
    val indexes = fields.map { f =>
      val idx =
        if (f.positions) PositionalIndex(buffers(f.name).toList)
        else FrequencyIndex(buffers(f.name).toList)
      (f.name, idx)
    }.toMap
    new MultiIndex(
      indexes = indexes,
      schema = schema,
      fields = storage.map { case (k, v) => (k, v.toArray) },
    )
  }
}
  
  """

  test("Searcher should return correct info for query sum") {
    val result = ScaladocSearcher.searchScaladoc(source, List("sum"))

    val actual = result.head

    val expected = ScaladocInfo(
      "sum",
      "This function sums two integers.",
      List("""@deprecated("Use add instead", "1.0")"""),
      List(),
      List("T: The type parameter", "a: The first parameter: Int", "b: The second parameter: Int"),
      "Int",
      17,
      18,
    )

    assertEquals(actual, expected)
  }

  test("Searcher should return correct info for query greet") {
    val result = ScaladocSearcher.searchScaladoc(source, List("greet"))

    val actual = result.head

    val expected = ScaladocInfo(
      "greet",
      "This function greets the user.",
      List("@throws(classOf[IllegalArgumentException])"),
      List(),
      List("name: The name parameter: String"),
      "Unit",
      24,
      25,
    )

    assertEquals(actual, expected)
  }

  test("Searcher should return correct info for query subtraction") {

    val result = ScaladocSearcher.searchScaladoc(source, List("subtraction"))

    val actual = result.head

    val expected = ScaladocInfo(
      "subtraction",
      "This function subtracts two integers.",
      List(),
      List(),
      List(
        "T: The type parameters",
        "c: The first parameter to subtract: Int",
        "d: The second parameter to subtract: Int",
      ),
      "Int",
      34,
      34,
    )

    assertEquals(actual, expected)
  }

  test("Parser should parse given code of sum correctly") {

    val result = ParseScaladoc.parseAndExtractInfo(parseTestSum)

    val actual = result.head

    val expected = ScaladocInfo(
      "sum",
      "This function sums two integers.",
      List("""@deprecated("Use add instead", "1.0")"""),
      List(),
      List("T: The type parameter", "a: The first parameter: Int", "b: The second parameter: Int"),
      "Int",
      8,
      9,
    )

    assertEquals(actual, expected)
  }

  test("Parser should parse given code of fromList correctly") {

    val result = ParseScaladoc.parseAndExtractInfo(parseTestfromList)

    val actual = result.head

    val expected = ScaladocInfo(
      "fromList",
      "An intermediate helper for iterating over documents and building an Index",
      List(),
      List(),
      List("docs: List[A]"),
      "MultiIndex",
      12,
      37,
    )

    assertEquals(actual, expected)
  }
}
