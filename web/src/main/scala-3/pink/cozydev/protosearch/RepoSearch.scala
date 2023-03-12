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

import pink.cozydev.protosearch.analysis.{Analyzer, QueryAnalyzer}

case class Hit(repo: Repo, score: Double)

object RepoSearch extends IOWebApp {

  def renderList(search: String => Either[String, List[Hit]]): Resource[IO, HtmlDivElement[IO]] =
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
          ol(
            cls := "results",
            children <-- queryStr.map { q =>
              val resultElems = search(q).map(rs => rs.map(renderListElem))
              resultElems.fold(err => List(renderError(err)), identity)
            },
          ),
        ),
      )
    }

  def renderListElem(hit: Hit): Resource[IO, HtmlLiElement[IO]] =
    li(
      div(
        cls := "card",
        div(
          cls := "card-content",
          div(
            cls := "level",
            div(
              cls := "level-left",
              p(cls := "title", a(href := hit.repo.url, hit.repo.name)),
            ),
            div(
              cls := "level-right has-text-grey-light",
              p(f"${hit.score * 1000}%.2f"),
            ),
          ), // level
          p(
            cls := "subtitle",
            span(hit.repo.fullName),
            span(s"  ✩ ${hit.repo.stars}"),
          ),
          p(cls := "subtitle", hit.repo.description),
        ),
      )
    )

  def renderError(err: String): Resource[IO, HtmlLiElement[IO]] =
    li(
      div(
        cls := "card has-background-danger-light",
        div(
          cls := "card-content",
          p(cls := "title", "Error"),
          p(cls := "subtitle has-text-danger", err),
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

    val analyzer = Analyzer.default.withLowerCasing
    val qAnalyzer = QueryAnalyzer(
      "description",
      ("name", analyzer),
      ("fullName", analyzer),
      ("description", analyzer),
      ("topics", analyzer),
    )

    def searchBldr(repos: List[Repo]): String => Either[String, List[Hit]] = {
      val index = MultiIndex.apply[Repo](
        "description",
        ("name", _.name, analyzer),
        ("fullName", _.fullName, analyzer),
        ("description", _.description.getOrElse(""), analyzer),
        ("topics", _.topics.mkString(" "), analyzer),
      )(repos)
      val scorer = Scorer(index)
      qs =>
        if (qs.isEmpty) Right(Nil)
        else {
          val aq = qAnalyzer.parse(qs)
          val results: Either[String, List[(Int, Double)]] = aq.flatMap { q =>
            val docScores = index.search(q).flatMap(ds => scorer.score(q, ds.toSet))
            docScores.map(ds => ds.sortBy(-_._2))
          }
          results.map(hits => hits.map((i, score) => Hit(repos(i), score)))
        }
    }

    Resource
      .eval(repos)
      .flatMap(repos => renderList(searchBldr(repos)))
      .flatTap(_ => Resource.eval(IO.println("FIN")))
  }

}
