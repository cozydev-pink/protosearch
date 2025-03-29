package pink.cozydev.protosearch.internal2

import java.util.regex.Pattern

// And(Term("cats"), Term("dog"))
// And(1, 2)            // posting indices
// AndList([1, 2], [3]) // postings

// Or(Term("cats"), Prefix("do"))
// Or(1, [2, 5, 7, 9...])
// OrList(1, {1, 2, 3, 4...}) // do we need to record how many times a docId occured?

sealed trait Occur
object Occur {
  case object MUST extends Occur
  // Like MUST but not considered in scoring
  case object FILTER extends Occur
  case object SHOULD extends Occur
  // Opposite of MUST and not considered in scoring
  case object MUSTNOT extends Occur
}

// We have not yet looked up any terms
// We can go straight from a Lucille Query to QueryTree
// We have resolved Fields, and eliminated any query types we don't support
sealed abstract class InternalTermQuery
object InternalTermQuery {
  case class Term(idx: String)

  case class Prefix(value: String)
  case class Regex(pattern: Pattern)
  case class TermRange(
      lower: String,
      upper: String,
      lowerInc: Boolean,
      upperInc: Boolean,
  )
}

case class BooleanClause(query: QueryTree, occur: Occur)

// TODO need scoring requirements here
// Various ordered arrays of posting indices
// TODO perhaps just PostingList instead of Int?
sealed abstract class QueryTree
object QueryTree {
  type Postings = Array[Int]

  case class Positional(
      postings: Array[Postings],
      positions: Array[Int],
  )

  // Boolean Queries
  case class Boolean(values: Array[BooleanClause], minShouldMatch: Int)
}
