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
import pink.cozydev.lucille.Parser

object MultiSearchApp extends IOApp.Simple {
  import BookIndex.{Book, corpus}

  val analyzer = Analyzer.default.withLowerCasing

  val index = MultiIndex.apply[Book](
    ("title", _.title, analyzer),
    ("author", _.author, analyzer),
  )(corpus)

  def search(qs: String): Either[String, List[Book]] = {
    val q = Parser.parseQ(qs).leftMap(_.toString)
    val result = q.flatMap(index.search)
    // TODO vector index access is unsafe
    result.map(hits => hits.map(i => corpus(i)))
  }

  val msg =
    s"""|Books:
        |${corpus.mkString("\n")}
        |
        |Try searching with a query like 'author:Suess'
        |
        | --  To quit enter ':q' and then press Ctrl + C   --
        |""".stripMargin

  def printResults(bs: List[Book]): String = {
    val bookLines = bs.map(b => s"  $b\n")
    s"""|Results:
        |${bookLines.mkString}
        | 
        |""".stripMargin
  }

  val prompt: IO[Unit] = for {
    _ <- IO.print("q> ")
    qs <- IO.readLine
    _ <- if (qs == ":q") IO(System.exit(0)) else IO.unit
    res = search(qs)
    _ <- res match {
      case Left(err) => IO.println(s"ERROR: $err")
      case Right(hits) => IO.println(printResults(hits))
    }
  } yield ()

  val run = IO.println(msg) *> prompt.foreverM
}
