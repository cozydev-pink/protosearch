package pink.cozydev.protosearch.internal

import pink.cozydev.lucille.Query
import pink.cozydev.protosearch.PositionalIndex
import cats.syntax.all._

// This is a mutable structure that callers will repeated call `next()` on.
abstract class MeowMeow {
  // This is going to represent a node in the "Query Tree"
  // Where the whole tree, and it's various nodes acts like an iterator
  // This iterator both matches and can score documents

  // A parent iterator can request the next match and specify a minimum matching
  // docID to consider, we can thus skip over other documents that we might match,
  // but which other iterators will not match
  def next(docId: Int): Int
}

// TODO Where does this class start?
// We could take in the `Query.Phrase`, or the list of terms and positions
// Or perhaps we take in our own Query representation?
class PhraseMeowMeow(
    val terms: Array[String], // TODO remove
    val postings: Array[PositionalPostingsReader],
    val relativePositions: Array[Int],
) extends MeowMeow {
  // TODO can this not be a concrete collection?
  // Could it not just be pointers into the tfData?
  // The ordering here perhaps matters. I think we want them ordered by frequency or length.
  // The most infrequent terms should be checked first to enable quick short circuiting

  val positionArr = new Array[Int](postings.length)

  def allDocsMatch: Boolean = {
    val res = postings.forall(p => p.currentDocId() == postings(0).currentDocId())
    println(s"allDocsMatch: ${postings.map(_.currentDocId()).toList}, returning $res")
    res
  }

  // TODO ....do
  // TODO for assume no "slop"
  def positionsMatch: Boolean =
    // Check that each position is satisfying it's relative position
    true

  def spanPos: (Int, Int) = (positionArr.min, positionArr.max)

  // #phrase - next "green" 9
  // green - 9,7
  // #phrase - next "eggs" 9
  // eggs - 9,8
  // #phrase - match 9:7,8

  private var currDocId: Int = 0
  private var inMatch: Boolean = false

  def printPosting(i: Int): String =
    s"i=$i term=${terms(i)}, posting=${postings(i)}"

  def next(docId: Int): Int = {
    var i = 0
    currDocId = docId
    // advance all postings until they are in match position
    while (i < postings.size && !allDocsMatch) {
      println(printPosting(i))
      val posting = postings(i)
      // if (!posting.hasNext) {
      //   println(s"Exiting while-loop early, i=$i, posting=$posting")
      //   return -1
      // }
      val di = posting.nextDoc(currDocId)
      if (di != currDocId) {
        // that posting didn't have a match at currDocId
        println("start back at the top of the postings list")
        i = 0
        currDocId = di
      } else {
        i += 1
      }
    }
    if (allDocsMatch) {
      inMatch = true
    }
    println(s"finished while-loop with currDocId=$currDocId")
    // All PositionReaders at the same docID
    // If so, check their relative positions
    if (positionsMatch) {
      postings(0).currentDocId()
    } else -1
  }
}
object PhraseMeowMeow {
  // TODO do we want this to live here? It makes this file depend on Lucille and the index
  def exact(index: PositionalIndex, q: Query.Phrase): Option[PhraseMeowMeow] = {
    val terms = q.str.split(" ")
    val relativePositions = (0 to terms.size).toArray
    val maybePostings = terms.toList.traverse(t => index.postingForTerm(t))
    maybePostings.map(ps =>
      new PhraseMeowMeow(terms, ps.map(_.reader()).toArray, relativePositions)
    )
  }
}
