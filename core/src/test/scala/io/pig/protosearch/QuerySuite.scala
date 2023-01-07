package io.pig.protosearch

class QuerySuite extends munit.FunSuite {

  val index = CatIndex.index

  test("AndQ") {
    val q = AndQ(List("fast", "cat"))
    assertEquals(q.search(index), List((1, 0.9241962407465937)))
  }

  test("OrQ") {
    val q = OrQ(List("fast", "cat"))
    val results = List(
      (0, 0.23104906018664842),
      (1, 0.9241962407465937),
      (2, 0.23104906018664842),
    )
    assertEquals(q.search(index), results)
  }

}
