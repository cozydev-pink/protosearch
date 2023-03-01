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

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuilder
import scala.collection.mutable.ListBuffer

sealed abstract class TermIndexArray private (
    val termDict: Vector[String],
    val tfData: Vector[Vector[Int]],
    val numDocs: Int,
) {

  val numTerms = termDict.size
  lazy val numData = tfData.map(_.size).sum / 2

  override def toString(): String = s"TermIndexArray($numTerms terms, $numData term-doc pairs)"

  def docCount(term: String): Int = {
    val idx = termIndex(term)
    if (idx < 0) 0
    else tfData(idx).size / 2
  }

  def docsWithTerm(term: String): List[Int] = {
    val idx = termIndex(term)
    if (idx < 0) Nil
    else evenElems(tfData(idx))
  }

  def docsWithTermSet(term: String): Set[Int] = {
    val idx = termIndex(term)
    // println(s"looking for term: $term, idx: $idx")
    if (idx < 0) Set.empty
    else {
      val x = Set.from(evenElems(tfData(idx)))
      // println(s"returning set: $x")
      x
    }
  }

  /** Find where the term would fit in the term list. */
  def termIndexWhere(term: String): Int = {
    val idx = termDict.indexWhere(_ >= term)
    if (idx == -1) termDict.length else idx
  }

  /** Get the list of terms between left and right. */
  def termsForRange(left: String, right: String): List[String] = {
    val li = termIndexWhere(left)
    val ri = termIndexWhere(right)
    termDict.slice(li, ri).toList
  }

  /** For every term between left and right, get the docs using those terms. */
  def docsForRange(left: String, right: String): Set[Int] = {
    import scala.collection.mutable.HashSet
    val bldr = HashSet.empty[Int]
    Range(termIndexWhere(left), termIndexWhere(right))
      .foreach(i => bldr.addAll(evenElems(tfData(i))))
    bldr.toSet
  }

  def scoreTFIDF(docs: Set[Int], term: String): List[(Int, Double)] =
    if (docs.size == 0) Nil
    else {
      val bldr = ListBuffer.newBuilder[(Int, Double)]
      bldr.sizeHint(docs.size)
      docs.foreach { docId =>
        val idx = termIndex(term)
        val arr = tfData(idx)
        val i = indexForDocId(arr, docId)
        if (i >= 0) {
          val tf = Math.log(1.0 + arr(i + 1))
          val idf: Double = 2.0 / arr.size.toDouble
          val tfidf: Double = tf * idf
          // println(s"term($term) doc($docId) tf: $tf, idf: $idf, tfidf: $tfidf")
          bldr += (docId -> tfidf)
        }
      }
      bldr.result().sortBy(-_._2).toList
    }

  def docsWithTermTFIDF(term: String): List[(Int, Double)] = {
    val idx = termIndex(term)
    if (idx < 0) Nil
    else {
      val arr = tfData(idx)
      var i = 0
      val bldr = ListBuffer.newBuilder[(Int, Double)]
      bldr.sizeHint(arr.size / 2)
      while (i < arr.length) {
        val id = arr(i)
        val tf = Math.log(1.0 + arr(i + 1))
        val idf: Double = 2.0 / arr.size.toDouble
        val tfidf: Double = tf * idf
        // println(s"tf: $tf, idf: $idf, tfidf: $tfidf")
        bldr += (id -> tfidf)
        i += 2
      }
      bldr.result().sortBy(-_._2).toList
    }
  }

  private def termIndex(term: String): Int =
    binarySearch(term, 0, numTerms)

  @tailrec
  private def binarySearch(elem: String, from: Int, to: Int): Int =
    if (to <= from) -1 // term doesn't exist, prefix search should start around here
    else {
      val idx = from + (to - from - 1) / 2
      math.signum(elem.compareTo(termDict(idx))) match {
        case -1 => binarySearch(elem, from, idx)
        case 1 => binarySearch(elem, idx + 1, to)
        case _ => idx
      }
    }

  private def evenElems(arr: Vector[Int]): List[Int] = {
    require(
      arr.size >= 2 && arr.size % 2 == 0,
      "evenElems expects even sized arrays of 2 or greater",
    )
    if (arr.size == 2) arr(0) :: Nil
    else {
      var i = 0
      val bldr = ListBuffer.newBuilder[Int]
      bldr.sizeHint(arr.size / 2)
      while (i < arr.length) {
        bldr += arr(i)
        i += 2
      }
      bldr.result().toList
    }
  }

  private def indexForDocId(arr: Vector[Int], id: Int): Int = {
    var i = 0
    while (i < arr.length) {
      if (arr(i) == id) return i
      i += 2
    }
    -1
  }

}
object TermIndexArray {
  import scala.collection.mutable.{TreeMap => MMap}
  import scala.collection.mutable.Stack

  def unsafeFromTuple3(
      num_data_terms: (Int, Vector[Vector[Int]], Vector[String])
  ): TermIndexArray = {
    val numDocs = num_data_terms._1
    val tfData = num_data_terms._2
    val termDict = num_data_terms._3
    new TermIndexArray(termDict, tfData, numDocs) {}
  }

  // don't want to take in Stream[F, Stream[F, A]] because we should really be taking in
  // a Stream[F, A] with evidence of Indexable[A]
  def apply(docs: Vector[Vector[String]]): TermIndexArray = {
    val m = new MMap[String, Stack[Int]].empty
    var docId = 0
    val docLen = docs.length
    docs.foreach { doc =>
      doc.foreach { term =>
        val s = m.getOrElseUpdate(term, Stack.empty)
        if (s.isEmpty) {
          // println(s"doc($docId), term($term), init freq = 1")
          s.prepend(1).prepend(docId)
        } else {
          val lastDoc = s.pop()
          val freq = s.pop()
          if (lastDoc == docId) {
            //   println(s"doc($docId), term($term), INCREMENT newFreq=${freq + 1}")
            s.prepend(freq + 1).prepend(docId)
          } else {
            //   println(s"doc($docId), term($term) new doc! init freq = 1")
            s.prepend(freq).prepend(lastDoc).prepend(1).prepend(docId)
          }
        }
      }
      docId += 1
    }
    val keys = ArrayBuilder.make[String]
    val values = ArrayBuilder.make[Vector[Int]]
    val size = m.size
    keys.sizeHint(size)
    values.sizeHint(size)
    m.foreachEntry { (k, v) =>
      keys.addOne(k)
      values.addOne(v.toVector)
    }
    new TermIndexArray(keys.result().toVector, values.result().toVector, docLen) {}
  }
}
