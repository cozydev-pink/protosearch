package pink.cozydev.protosearch.internal

abstract class QueryIterator extends Iterator[Int] {
  def currentDocId: Int
  def currentScore: Float

  def isMatch: Boolean

  // TODO can this be currentDocId != -1 ?
  def hasNext: Boolean

  def next(): Int = advance(currentDocId + 1)

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
  def advance(docId: Int) = qi.advance(docId)
}

class ConstantScoreQueryIterator(qi: QueryIterator, score: Int) {
  def currentDocId = qi.currentDocId
  val currentScore = score
  def isMatch = qi.isMatch
  def hasNext = qi.hasNext
  def advance(docId: Int) = qi.advance(docId)
}

class NoMatchQueryIterator extends QueryIterator {
  val currentDocId = -1
  val currentScore = 0.0f
  val isMatch = false
  val hasNext = false
  def advance(docId: Int) = -1
  override def docs: Iterator[Int] = Iterator.empty
}
