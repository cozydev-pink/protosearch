package io.pig.protosearch

class IngestSuite extends munit.FunSuite {
  val doc = """|
               |# Title
               |
               |sub title line
               |
               |## H2 Section
               |
               |h2 section text
               |
               |## Second H2
               |
               |second section text
               |""".stripMargin

  test("laika extracts headings") {
    val x = Ingest.transform(doc)
    val headings = List("H2 Section", "Second H2", "Title")
    assertEquals(x, Right(headings))
  }

}
