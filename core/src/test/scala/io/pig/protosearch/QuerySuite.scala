package io.pig.protosearch

import io.pig.lucille.Parser

class QuerySuite extends munit.FunSuite {

  val index = CatIndex.index

  test("AndQ") {
    val q = Parser.parseQ("fast AND cat").map(_.head)
    assertEquals(
      q.flatMap(q => BooleanQuery.search(index, q)),
      Right(List((1, 0.9241962407465937))),
    )
  }

  test("OrQ") {
    val q = Parser.parseQ("fast OR cat").map(_.head)
    val results = List(
      (0, 0.23104906018664842),
      (1, 0.9241962407465937),
      (2, 0.23104906018664842),
    )
    assertEquals(q.flatMap(q => BooleanQuery.search(index, q)), Right(results))
  }

}
