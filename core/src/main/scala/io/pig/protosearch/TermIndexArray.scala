package io.pig.protosearch

import scala.annotation.tailrec
import scala.collection.mutable.ArrayBuilder
import scala.collection.mutable.ListBuffer

sealed abstract class TermIndexArray private (
    private val termDict: Vector[String],
    private val tfData: Vector[Vector[Int]],
) {

  val numTerms = termDict.size

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

}
object TermIndexArray {
  import scala.collection.mutable.{TreeMap => MMap}
  import scala.collection.mutable.Stack

  def apply(docs: List[List[String]]): TermIndexArray = {
    val m = new MMap[String, Stack[Int]].empty
    var docId = 0
    val docLen = docs.length
    while (docId < docLen) {
      docs(docId).foreach { term =>
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
    new TermIndexArray(keys.result().toVector, values.result().toVector) {}
  }
}
