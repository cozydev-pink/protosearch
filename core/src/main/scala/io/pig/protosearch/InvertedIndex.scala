package io.pig.protosearch

class InvertedIndex(underlying: Map[String, Set[Int]]) {

  val numTerms = underlying.size

  def docCount(term: String): Int =
    underlying.get(term).map(_.size).getOrElse(0)

  def docsWithTerm(term: String): List[Int] =
    underlying.get(term).map(_.toList).getOrElse(Nil)
}
object InvertedIndex {
  import scala.collection.mutable.{HashMap => MMap}
  import scala.collection.mutable.{BitSet => MBitSet}
  def apply(docs: List[List[String]]): InvertedIndex = {
    val m = new MMap[String, MBitSet](
      initialCapacity = docs.size + docs.head.size,
      loadFactor = MMap.defaultLoadFactor,
    )
    var docId = 0
    val docLen = docs.length
    while (docId < docLen) {
      docs(docId).foreach { term =>
        val bldr = m.getOrElseUpdate(term, MBitSet.empty)
        bldr += docId
      }
      docId += 1
    }
    new InvertedIndex(m.view.mapValues(_.toSet).toMap)
  }
}
