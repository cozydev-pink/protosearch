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

import cats.effect.IOApp
import cats.effect.IO
import cats.syntax.all._
import TokenStream.tokenizeSpaceV
import pink.cozydev.lucille.Parser

object MultiSearchApp extends IOApp.Simple {

  case class Book(title: String, author: String) {
    override def toString = s"\"$title\" by $author"
  }

  val corpus: Vector[Book] = Vector(
    Book("The Tale of Peter Rabbit", "Beatrix Potter"),
    Book("The Tale of Two Bad Mice", "Beatrix Potter"),
    Book("One Fish, Two Fish, Red Fish, Blue Fish", "Dr. Suess"),
    Book("Green Eggs and Ham", "Dr. Suess"),
  )

  val index = MultiIndex.apply[Book](
    ("title", d => tokenizeSpaceV(d.title)),
    ("author", d => tokenizeSpaceV(d.author)),
  )(corpus)

  def search(qs: String): Either[String, List[Book]] = {
    val q = Parser.parseQ(qs).leftMap(_.toString)
    val result = q.flatMap(index.search)
    // TODO vector index access is unsafe
    result.map(hits => hits.map(i => corpus(i)))
  }

  val msg = IO.println(s"Books:\n${corpus.mkString("\n")}\n\n") *>
    IO.println("Try searching with a query like 'author:Suess'\n\n")

  def printResults(bs: List[Book]): String = {
    val bookLines = bs.map(b => s"  $b\n")
    s"""|Results:
        |${bookLines.mkString}
        | 
        |""".stripMargin
  }

  val loop = for {
    qs <- IO.print("q> ") *> IO.readLine
    res = search(qs)
    _ <- res match {
      case Left(err) => IO.println(s"ERROR: $err")
      case Right(hits) => IO.println(printResults(hits))
    }
  } yield ()
  val run = msg *> loop.foreverM
}
