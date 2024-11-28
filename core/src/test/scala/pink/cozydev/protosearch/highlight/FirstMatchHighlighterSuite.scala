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
  val formatter = FragmentFormatter(36, "<b>", "</b>")
  val highlighter = FirstMatchHighlighter(formatter)

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

  test("highlights only first match") {
    val s = "hello world, you, nice world, you"
    val actual = highlighter.highlight(s, "world")
    val expected = "hello <b>world</b>, you, nice world, you"
    assertEquals(actual, expected)
  }

  test("highlights matches near end of long doc") {
    val s = List.fill(100)("hello cat,").mkString("", " ", " world")
    val actual = highlighter.highlight(s, "world")
    val expected = "cat, hello cat, hello cat, <b>world</b>"
    assertEquals(actual, expected)
  }

  test("long docs get trimmed with ellipses") {
    val s = List.fill(100)("hello cat,").mkString("", " ", " world")
    val actual = highlighter.highlight(s, "fake")
    val expected = s.take(formatter.maxSize) + "..."
    assertEquals(actual, expected)
  }
}
