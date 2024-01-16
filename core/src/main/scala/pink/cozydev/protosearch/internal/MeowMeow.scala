package pink.cozydev.protosearch.internal

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
class PhraseMeowMeow (
    val terms: Array[String],
    val relativePositions: Array[Int]
) extends MeowMeow {

  // TODO can this not be a concrete collection?
  // Could it not just be pointers into the tfData?
  // The ordering here perhaps matters. I think we want them ordered by frequency or length.
  // The most infrequent terms should be checked first to enable quick short circuiting
  val postings: Array[PositionalPostingsReader] = Array.empty

  val positionArr = new Array[Int](terms.length)

  def allDocsMatch: Boolean =
    postings.forall(p => p.currentDocId() == postings(0).currentDocId())

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

  var currDocId: Int = 0

  def next(docId: Int): Int = {
    var i = 0
    currDocId = docId
    // advance all postings until they are in match position
    while (i <= postings.size && !allDocsMatch) {
      val di = postings(i).nextDoc(currDocId)
      if (di == -1) {
        // no more matches in this posting
        // return -1
      }
      if (di != -1 && di != currDocId) {
        // that posting didn't have a match at currDocId
        // start back at the top of the postings list
        i = 0
        currDocId = di
      }
      i += 1
    }
    // All PositionReaders at the same docID
    // If so, check their relative positions
    if (positionsMatch) {
      //
      currDocId
    } else -1
  }
}