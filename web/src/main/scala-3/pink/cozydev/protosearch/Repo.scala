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

import cats.effect.IO
import fs2.Stream
import fs2.io.file.{Files, Path}
import io.circe.{Decoder, Encoder, HCursor, Json}
import io.circe.fs2.*

final case class Repo(
    name: String,
    fullName: String,
    description: String,
    url: String,
    homepage: Option[String],
    stars: Int,
    topics: List[String],
)
object Repo {
  implicit val decodeRepo: Decoder[Repo] = new Decoder[Repo] {
    final def apply(c: HCursor): Decoder.Result[Repo] =
      for {
        name <- c.downField("name").as[String]
        fullName <- c.downField("full_name").as[String]
        desc <- c.downField("description").as[String]
        url <- c.downField("html_url").as[String]
        h <- c.downField("homepage").as[Option[String]]
        homepage = h.filter(_.nonEmpty)
        stars <- c.downField("stargazers_count").as[Int]
        topics <- c.downField("topics").as[List[String]]
      } yield Repo(name, fullName, desc, url, homepage, stars, topics)
  }

  def parseRepos(bytes: Stream[IO, Byte]): Stream[IO, Repo] =
    bytes.through(byteStreamParser).through(decoder[IO, Repo])

}
