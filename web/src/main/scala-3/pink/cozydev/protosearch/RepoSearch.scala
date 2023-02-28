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

import cats.syntax.all._
import calico.*
import calico.html.io.{*, given}
import cats.effect.*
import fs2.Stream
import fs2.dom.*
import fs2.concurrent.SignallingRef
import org.http4s.{Request, Method}
import org.http4s.dom.FetchClientBuilder
import org.http4s.implicits.uri
import org.http4s.circe.CirceEntityCodec._

object RepoSearch extends IOWebApp {

  def renderList(search: String => List[Repo]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("").toResource.flatMap { queryStr =>
      div(
        cls := "columns",
        div(
          cls := "column is-8 is-offset-2",
          sectionTag(
            cls := "section",
            input.withSelf { self =>
              (
                cls := "input is-medium",
                typ := "text",
                placeholder := "search repos...",
                onInput --> (_.foreach(_ => self.value.get.flatMap(queryStr.set))),
              )
            },
          ),
          ol(cls := "results", children <-- queryStr.map(q => search(q).map(result))),
        ),
      )
    }

  def result(repo: Repo): Resource[IO, HtmlLiElement[IO]] =
    li(
      div(
        cls := "card",
        div(
          cls := "card-content",
          p(cls := "title", repo.name),
          p(cls := "subtitle", repo.description),
        ),
      )
    )

  def render: Resource[IO, HtmlElement[IO]] = {
    val client = FetchClientBuilder[IO].create
    val repos: IO[List[Repo]] =
      (Stream.eval(IO.println("fetching data...")) >>
        client
          .stream(Request[IO](Method.GET, uri"/repo-dataset.jsonl"))
          .flatMap(r => r.body.through(Repo.parseRepos))).compile.toList

    Resource
      .eval(repos)
      .flatMap(repos => renderList(q => repos.filter(_.name == q)))
      .flatTap(_ => Resource.eval(IO.println("FIN")))
  }

}
