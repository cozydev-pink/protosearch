package pink.cozydev.protosearch

sealed trait SearchResult {
  def fold[A](fail: String => A, success: List[Hit] => A): A = this match {
    case SearchFailure(msg) => fail(msg)
    case SearchSuccess(hits) => success(hits)
  }
}
final case class SearchFailure(msg: String) extends SearchResult
final case class SearchSuccess(hits: List[Hit]) extends SearchResult
