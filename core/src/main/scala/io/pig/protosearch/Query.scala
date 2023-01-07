package io.pig.protosearch

import cats.data.NonEmptyList
import cats.syntax.all._
import io.pig.lucille.Query

object BooleanQuery {

  def search(index: TermIndexArray, q: Query): Either[String, List[(Int, Double)]] = {
    val terms = onlyTerms(NonEmptyList.of(q))
    val docs = booleanModel(index, q)
    terms.flatMap(terms => docs.map(ds => BooleanQueryImpl.scoreEm(index, terms, ds)))
  }

  def booleanModel(index: TermIndexArray, q: Query): Either[String, Set[Int]] =
    q match {
      case Query.OrQ(qs) => onlyTerms(qs).map(qs => BooleanQueryImpl.OrQ(qs).search(index))
      case Query.AndQ(qs) => onlyTerms(qs).map(qs => BooleanQueryImpl.AndQ(qs).search(index))
      case Query.TermQ(q) => Right(BooleanQueryImpl.termQ(index, q))
      case Query.PhraseQ(_) => Left("Phrase queries require position data, which we don't have yet")
      case _: Query.FieldQ => Left("We only have one implicit field currently")
      case _ => Left("Bro, c'mon, only ORs and ANDs, thank you")
    }

  private def onlyTerms(qs: NonEmptyList[Query]): Either[String, NonEmptyList[String]] =
    qs.flatTraverse {
      case Query.OrQ(qs) => onlyTerms(qs)
      case Query.AndQ(qs) => onlyTerms(qs)
      case Query.TermQ(t) => Right(NonEmptyList.of(t))
      case x => Left(s"Sorry bucko, only term queries supported today, not $x")
    }
}

object BooleanQueryImpl {
  def scoreEm(
      index: TermIndexArray,
      terms: NonEmptyList[String],
      docs: Set[Int],
  ): List[(Int, Double)] =
    terms.toList
      .flatMap(t => index.scoreTFIDF(docs, t))
      .groupMapReduce(_._1)(_._2)(_ + _)
      .toList

  def termQ(index: TermIndexArray, t: String): Set[Int] =
    index.docsWithTermSet(t)

  case class OrQ(terms: NonEmptyList[String]) {
    def search(index: TermIndexArray): Set[Int] =
      terms.toList.map(t => index.docsWithTermSet(t)).reduce(_ union _)
  }

  case class AndQ(terms: NonEmptyList[String]) {
    def search(index: TermIndexArray): Set[Int] =
      terms.toList.map(t => index.docsWithTermSet(t)).reduce(_ intersect _)
  }
}
