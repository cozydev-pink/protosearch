package pink.cozydev.protosearch.internal

/** A stateful reader for `PositionalPostingsList`s, tracking the `currentDocId` and
  * `currentPosition` as it iterates through the postings.
  */
private[internal] abstract class PositionalPostingsReader extends FrequencyPostingsReader {
  def currentDocId: Int
  def currentFrequency: Int
  def currentPosition: Int
  def hasNext: Boolean
  def nextDoc(): Int

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def advance(docId: Int): Int

  def hasNextPosition: Boolean

  /** Advances and returns the next position if possible, returns -1 if there are no remaining positions.
    *
    * @return new `currentPosition` value
    */
  def nextPosition(): Int
  def nextPosition(target: Int): Int
}
