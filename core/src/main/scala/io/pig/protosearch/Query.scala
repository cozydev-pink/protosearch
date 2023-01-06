package io.pig.protosearch

case class OrQ(terms: List[String]) {

  def search(index: TermIndexArray): List[(Int, Double)] = {
    val docs = terms.map(t => index.docsWithTermSet(t)).reduce(_ union _)
    terms
      .flatMap(t => index.scoreTFIDF(docs, t))
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList
  }
}

case class AndQ(terms: List[String]) {

  def search(index: TermIndexArray): List[(Int, Double)] = {
    val docs = terms.map(t => index.docsWithTermSet(t)).reduce(_ intersect _)
    terms
      .flatMap(t => index.scoreTFIDF(docs, t))
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList
  }
}
