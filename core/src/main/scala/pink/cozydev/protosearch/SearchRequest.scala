package pink.cozydev.protosearch

final case class SearchRequest(
    query: String,
    size: Int,
    highlightFields: List[String],
    resultFields: List[String],
    lastTermPrefix: Boolean,
    // sort
    // query re-writing?
)
object SearchRequest {
  private val defaultSize = 10
  def default(query: String): SearchRequest =
    SearchRequest(query, defaultSize, Nil, Nil, false)
}
