package pink.cozydev.protosearch.scaladoc

object ScaladocSearcher {
  def searchScaladoc(source: String,queries: List[String]) : List[ScaladocInfo] = {
    val scaladocInfoList = ParseScaladoc.parseAndExtractInfo(source)
    val index = ScaladocIndexer.createScaladocIndex(scaladocInfoList)
    def search(q: String): List[ScaladocInfo] = {
        val searchResults = index.search(q)
        searchResults.fold(_ => Nil, hits => hits.map(h => scaladocInfoList.toList(h.id)))
    }
    
    val results: List[ScaladocInfo] = queries.flatMap { query =>
      search(query)
    }

    results
  }
}