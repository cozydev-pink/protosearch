package pink.cozydev.protosearch.highlight

case class FragmentFormatter(
    maxSize: Int,
    startTag: String,
    endTag: String,
) {
  private val tagSize = startTag.size + endTag.size
  private val tags: Array[String] = Array(startTag, endTag)

  def format(fragment: String, offsets: Iterable[Int]): String =
    if (offsets.size == 0) fragment
    else {
      val offsetArr = offsets.toArray
      assert(offsetArr.size % 2 == 0, "even number of offsets required")

      val sb = new StringBuilder()
      sb.sizeHint(fragment.size + tagSize * (offsets.size / 2))
      val chars = fragment.toCharArray()

      try {
        // Add initial characters
        sb.appendAll(chars, 0, offsetArr(0))
        var charsOffset = offsetArr(0)

        // Loop through offsets, two at a time
        var i = 0
        while (i < offsetArr.size) {
          val offsetStart = offsetArr(i)
          // add chars between offsets
          val inbetweenChars = offsetStart - charsOffset
          sb.appendAll(chars, charsOffset, inbetweenChars)
          charsOffset += inbetweenChars

          // start new offset
          sb.append(startTag)
          val offsetLength = offsetArr(i + 1)
          sb.appendAll(chars, charsOffset, offsetLength)
          sb.append(endTag)
          charsOffset += offsetLength
          i += 2
        }
        // Add remaining characters
        sb.appendAll(chars, charsOffset, chars.size - charsOffset)
        sb.result()
      } catch {
        case _: IndexOutOfBoundsException =>
          throw new IllegalArgumentException("Offset exceeded string length")
      }
    }
}
