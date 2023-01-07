package io.pig.protosearch

import cats.data.NonEmptyList
import cats.syntax.all._
import io.pig.lucille.Query

case class BooleanQuery(index: TermIndexArray, defaultOR: Boolean = true) {

  def search(q: Query): Either[String, List[(Int, Double)]] = {
    val terms = onlyTerms(NonEmptyList.of(q))
    val docs = booleanModel(q)
    terms.flatMap(terms => docs.map(ds => scoreEm(terms, ds)))
  }

  def booleanModel(q: Query): Either[String, Set[Int]] =
    q match {
      case Query.OrQ(qs) => qs.traverse(booleanModel).map(unionSets)
      case Query.AndQ(qs) => qs.traverse(booleanModel).map(intersectSets)
      case Query.Group(qs) => qs.traverse(booleanModel).map(defaultCombine)
      case Query.TermQ(q) => Right(index.docsWithTermSet(q))
      case Query.PhraseQ(_) => Left("Phrase queries require position data, which we don't have yet")
      case _: Query.FieldQ => Left("We only have one implicit field currently")
      case _ => Left("Bro, c'mon, only ORs and ANDs, thank you")
    }

  private def defaultCombine(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (defaultOR) unionSets(sets) else intersectSets(sets)

  private def intersectSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (sets.size == 1) sets.head
    else {
      val setList = sets.tail
      var s = sets.head
      var i = 0
      while (i < setList.size) {
        s = s.intersect(setList(i))
        i += 1
      }
      s
    }

  private def unionSets(sets: NonEmptyList[Set[Int]]): Set[Int] =
    if (sets.size == 1) sets.head
    else {
      val setList = sets.tail
      var s = sets.head
      var i = 0
      while (i < setList.size) {
        s = s.union(setList(i))
        i += 1
      }
      s
    }

  private def onlyTerms(queries: NonEmptyList[Query]): Either[String, NonEmptyList[String]] =
    queries.flatTraverse {
      case Query.OrQ(qs) => onlyTerms(qs)
      case Query.AndQ(qs) => onlyTerms(qs)
      case Query.TermQ(t) => Right(NonEmptyList.of(t))
      case Query.Group(qs) => onlyTerms(qs)
      case x => Left(s"Sorry bucko, only term queries supported today, not $x")
    }

  private def scoreEm(
      terms: NonEmptyList[String],
      docs: Set[Int],
  ): List[(Int, Double)] =
    terms.toList
      .flatMap(t => index.scoreTFIDF(docs, t))
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList
}
