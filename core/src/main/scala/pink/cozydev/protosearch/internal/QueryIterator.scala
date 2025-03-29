package pink.cozydev.protosearch.internal

abstract class QueryIterator extends Iterator[Int] {
  def currentDocId: Int
  def currentScore: Float

  def isMatch: Boolean

  // TODO can this be currentDocId != -1 ?
  def hasNext: Boolean

  // TODO Can these not be advance(currentDocId + 1) ?
  def nextDoc(): Int
  def next(): Int = nextDoc()

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def advance(docId: Int): Int

  def docs: Iterator[Int] = this.takeWhile(_ > -1)
}

class BoostQueryIterator(qi: QueryIterator, boost: Float) {
  def currentDocId = qi.currentDocId
  def currentScore = qi.currentScore * boost
  def isMatch = qi.isMatch
  def hasNext = qi.hasNext
  def nextDoc() = qi.nextDoc()
  def advance(docId: Int) = qi.advance(docId)
}

class ConstantScoreQueryIterator(qi: QueryIterator, score: Int) {
  def currentDocId = qi.currentDocId
  val currentScore = score
  def isMatch = qi.isMatch
  def hasNext = qi.hasNext
  def nextDoc() = qi.nextDoc()
  def advance(docId: Int) = qi.advance(docId)
}

object NoMatchQueryIterator extends QueryIterator {
  val currentDocId = -1
  val currentScore = 0.0f
  val isMatch = false
  val hasNext = false
  def nextDoc() = -1
  def advance(docId: Int) = -1
}
