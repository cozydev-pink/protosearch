package io.pig.protosearch

import scala.collection.mutable.ListBuffer

class InvertedIndexWithTF(underlying: Map[String, Array[Int]]) {

  val numTerms = underlying.size

  def docCount(term: String): Int =
    underlying.get(term).map(_.size).getOrElse(0) / 2

  private def evenElems(arr: Array[Int]): List[Int] = {
    var i = 0
    val bldr = ListBuffer.newBuilder[Int]
    bldr.sizeHint(arr.size / 2)
    while (i < arr.length) {
      bldr += arr(i)
      i += 2
    }
    bldr.result().toList
  }

  def docsWithTerm(term: String): List[Int] =
    underlying.get(term).map(evenElems).getOrElse(Nil)

}
object InvertedIndexWithTF {
  import scala.collection.mutable.{HashMap => MMap}
  import scala.collection.mutable.Stack

  def apply(docs: List[List[String]]): InvertedIndexWithTF = {
    val m = new MMap[String, Stack[Int]](
      initialCapacity = docs.size + docs.head.size,
      loadFactor = MMap.defaultLoadFactor,
    )
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
    new InvertedIndexWithTF(m.view.mapValues(_.toArray).toMap)
  }
}
