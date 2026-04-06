/*
 * Copyright 2022 CozyDev
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package pink.cozydev.protosearch.highlight

class FirstMatchHighlighterSuite extends munit.FunSuite {
  val formatter = FragmentFormatter("<b>", "</b>")
  val highlighter = FirstMatchHighlighter(formatter, 60)

  test("no highlight on no match") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "cat")
    val expected = "hello world"
    assertEquals(actual, expected)
  }

  test("highlights simple substring") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "world")
    val expected = "hello <b>world</b>"
    assertEquals(actual, expected)
  }

  test("highlights simple substring, ignoring trailing whitespace in query") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "world ")
    val expected = "hello <b>world</b>"
    assertEquals(actual, expected)
  }

  test("highlights simple substring, ignoring leading whitespace in query") {
    val s = "hello world"
    val actual = highlighter.highlight(s, " world")
    val expected = "hello <b>world</b>"
    assertEquals(actual, expected)
  }

  test("highlights substring with different casing") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "WoRlD")
    val expected = "hello <b>world</b>"
    assertEquals(actual, expected)
  }

  test("highlights first character") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "h")
    val expected = "<b>h</b>ello world"
    assertEquals(actual, expected)
  }

  test("highlights last character") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "d")
    val expected = "hello worl<b>d</b>"
    assertEquals(actual, expected)
  }

  test("highlights whole string") {
    val s = "hello world"
    val actual = highlighter.highlight(s, "hello world")
    val expected = "<b>hello world</b>"
    assertEquals(actual, expected)
  }

  test("highlights only first match") {
    val s = "hello world, you, nice world, you"
    val actual = highlighter.highlight(s, "world")
    val expected = "hello <b>world</b>, you, nice world, you"
    assertEquals(actual, expected)
  }

  val longDoc = List.fill(100)("hello cat,").mkString("Contents: A letter to cat\n", " ", " world")

  test("highlights first word match near beginning of long doc") {
    val actual = highlighter.highlight(longDoc, "Contents")
    val expected = "<b>Contents</b>: A letter to cat\nhello cat, hello cat, hello cat, h..."
    assertEquals(actual, expected)
  }

  test("highlights character in word near beginning of long doc") {
    val actual = highlighter.highlight(longDoc, "t")
    val expected = "Con<b>t</b>ents: A letter to cat\nhello cat, hello cat, hello cat, h..."
    assertEquals(actual, expected)
  }

  test("highlights word near beginning of long doc") {
    val actual = highlighter.highlight(longDoc, "cat")
    val expected = "Contents: A letter to <b>cat</b>\nhello cat, hello cat, hello cat, h..."
    assertEquals(actual, expected)
  }

  test("highlights matches near end of long doc") {
    val actual = highlighter.highlight(longDoc, "world")
    val expected = "cat, hello cat, hello cat, <b>world</b>"
    assertEquals(actual, expected)
  }

  test("highlights last character at end of long doc") {
    val actual = highlighter.highlight(longDoc, "d")
    val expected = "hello cat, hello cat, worl<b>d</b>"
    assertEquals(actual, expected)
  }

  test("long docs get trimmed with ellipses") {
    val actual = highlighter.highlight(longDoc, "fake")
    val expected = longDoc.take(highlighter.maxSize + formatter.tagSize) + "..."
    assertEquals(actual, expected)
  }

  test("highlights correctly when no word boundary after lookback starts") {
    val lookBack = highlighter.lookBackWindowSize
    // xxxx...xxxxxcatxxxx...
    //    ^-- lookback
    val s = "x" * (lookBack + 5) + "cat" + "x" * lookBack
    val actual = highlighter.highlight(s, "cat")
    assert(actual.contains("<b>cat</b>"))
  }

  test("trim does not produce broken highlight tags") {
    val maxSize = highlighter.maxSize
    // match near end, string just under maxSize so tags push it over
    val s = "x" * (maxSize - 6) + "cat" + "xx"
    val actual = highlighter.highlight(s, "cat")
    assert(s.size < maxSize)
    assert((s.size + formatter.startTag.size + formatter.endTag.size) > maxSize)
    assert(actual.contains("<b>") && actual.contains("</b>"))
  }

  test("highlights correctly when first word boundary appears after match") {
    val lookBack = highlighter.lookBackWindowSize
    // xxxxxxx...xxxxxcat xxxx...
    //    ^-- lookback   ^-- word boundary
    val s = "x" * (lookBack + 5) + "cat" + " " + "x" * lookBack
    val actual = highlighter.highlight(s, "cat")
    assert(actual.contains("<b>cat</b>"))
  }
}
