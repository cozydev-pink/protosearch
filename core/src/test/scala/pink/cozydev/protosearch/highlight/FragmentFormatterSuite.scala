package pink.cozydev.protosearch.highlight

import munit.FunSuite

class FragmentFormatterSuite extends FunSuite {
  val formatter = FragmentFormatter(100, "<b>", "</b>")

  test("formats string with empty offsets") {
    val s = "hello world"
    val actual = formatter.format(s, List.empty)
    val expected = s
    assertEquals(actual, expected)
  }

  test("formats string with one pair of offsets") {
    val s = "hello world"
    val actual = formatter.format(s, List(6, 5))
    val expected = "hello <b>world</b>"
    assertEquals(actual, expected)
  }

  test("formats string with offset in the middle") {
    val s = "hello world, how are you?"
    val actual = formatter.format(s, List(6, 5))
    val expected = "hello <b>world</b>, how are you?"
    assertEquals(actual, expected)
  }

  test("formats string with two pair of offsets") {
    val s = "hello world"
    val actual = formatter.format(s, List(0, 5, 6, 5))
    val expected = "<b>hello</b> <b>world</b>"
    assertEquals(actual, expected)
  }

  test("formats whole string if one offset") {
    val s = "hello world"
    val actual = formatter.format(s, List(0, 11))
    val expected = "<b>hello world</b>"
    assertEquals(actual, expected)
  }

  test("throws if offset length exceeds string boundary") {
    val s = "hello world"
    intercept[java.lang.IllegalArgumentException] {
      formatter.format(s, List(6, 5 + 1))
    }
  }

  test("throws if odd number of offset integers") {
    intercept[java.lang.AssertionError] {
      formatter.format("", List(1))
    }
  }
}
