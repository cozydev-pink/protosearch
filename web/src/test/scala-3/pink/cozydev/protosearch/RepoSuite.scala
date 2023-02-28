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

package pink.cozydev.protosearch

import io.circe._
import io.circe.parser.decode

class RepoSuite extends munit.FunSuite {

  test("parse 'cats' repo json") {
    val json =
      """|{
         |  "id": 29986727,
         |  "name": "cats",
         |  "full_name": "typelevel/cats",
         |  "description": "Lightweight, modular, and extensible library for functional programming.",
         |  "html_url": "https://github.com/typelevel/cats",
         |  "homepage": "https://typelevel.org/cats/",
         |  "stargazers_count": 4860,
         |  "topics": []
         |}""".stripMargin
    val r = decode[Repo](json)
    val expected = Repo(
      "cats",
      "typelevel/cats",
      Some("Lightweight, modular, and extensible library for functional programming."),
      "https://github.com/typelevel/cats",
      Some("https://typelevel.org/cats/"),
      4860,
      List.empty,
    )
    assertEquals(r, Right(expected))
  }

  test("parse 'cats' repo without homepage json") {
    val json =
      """|{
         |  "id": 29986727,
         |  "name": "cats",
         |  "full_name": "typelevel/cats",
         |  "description": "Lightweight, modular, and extensible library for functional programming.",
         |  "html_url": "https://github.com/typelevel/cats",
         |  "homepage": "",
         |  "stargazers_count": 4860,
         |  "topics": []
         |}""".stripMargin
    val r = decode[Repo](json)
    val expected = Repo(
      "cats",
      "typelevel/cats",
      Some("Lightweight, modular, and extensible library for functional programming."),
      "https://github.com/typelevel/cats",
      None,
      4860,
      List.empty,
    )
    assertEquals(r, Right(expected))
  }

  test("parse 'cats' repo with topics json") {
    val json =
      """|{
         |  "id": 29986727,
         |  "name": "cats",
         |  "full_name": "typelevel/cats",
         |  "description": "Lightweight, modular, and extensible library for functional programming.",
         |  "html_url": "https://github.com/typelevel/cats",
         |  "homepage": "https://typelevel.org/cats/",
         |  "stargazers_count": 4860,
         |  "topics": ["fp", "meowing"]
         |}""".stripMargin
    val r = decode[Repo](json)
    val expected = Repo(
      "cats",
      "typelevel/cats",
      Some("Lightweight, modular, and extensible library for functional programming."),
      "https://github.com/typelevel/cats",
      Some("https://typelevel.org/cats/"),
      4860,
      List("fp", "meowing"),
    )
    assertEquals(r, Right(expected))
  }

}
