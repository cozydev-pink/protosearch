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

package pink.cozydev.protosearch.fixtures

object BookIndex {
  case class Book(title: String, author: String) {
    override def toString = s"\'$title\' by $author"
  }

  val peter = Book("The Tale of Peter Rabbit", "Beatrix Potter")
  val mice = Book("The Tale of Two Bad Mice", "Beatrix Potter")
  val fish = Book("One Fish, Two Fish, Red Fish, Blue Fish", "Dr. Suess")
  val eggs = Book("Green Eggs and Ham", "Dr. Suess")

  val allBooks = List(peter, mice, fish, eggs)
}
