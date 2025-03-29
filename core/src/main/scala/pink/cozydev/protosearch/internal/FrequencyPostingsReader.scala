package pink.cozydev.protosearch.internal

/** A stateful reader for `FrequencyPostingsList`s, tracking the `currentDocId` and
  * `currentPosition` as it iterates through the postings.
  */
private[internal] abstract class FrequencyPostingsReader {
  def currentDocId: Int
  def currentFrequency: Int
  def hasNext: Boolean
  def nextDoc(): Int

  /** Advances until `docId` or greater if possible, skipping docs less than `docId`.
    * Does not advance if already at `docId`.
    * @return new `currentDocId` value
    */
  def advance(docId: Int): Int
}
