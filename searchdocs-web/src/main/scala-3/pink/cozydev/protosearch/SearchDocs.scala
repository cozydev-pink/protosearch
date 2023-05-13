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

object SearchDocs extends IOWebApp {

  import DocumentationSearch._

  def docToLink(doc: Doc): String = {
    val file = doc.fileName.stripSuffix(".md")
    val path = "https://http4s.org/v0.23/docs/" + file
    doc.anchor match {
      case None => path
      case Some(section) => path + "#" + section
    }
  }

  def renderList(search: String => Either[String, List[Hit]]): Resource[IO, HtmlDivElement[IO]] =
    SignallingRef[IO].of("").toResource.flatMap { queryStr =>
      div(
        cls := "container is-widescreen",
        sectionTag(
          cls := "section",
          input.withSelf { self =>
            (
              cls := "input is-primary is-medium",
              typ := "text",
              placeholder := "search docs...",
              onInput --> (_.foreach(_ => self.value.get.flatMap(queryStr.set))),
            )
          },
        ),
        ol(
          cls := "results",
          children <-- queryStr.map { q =>
            val resultElems = search(q).map {
              case Nil => List(renderNoResult)
              case rs => rs.map(renderListElem)
            }
            resultElems.fold(err => List(renderError(err)), identity)
          },
        ),
      )
    }

  def renderListElem(hit: Hit): Resource[IO, HtmlUListElement[IO]] =
    ul(
      div(
        cls := "card",
        div(
          cls := "card-content",
          p(
            cls := "is-size-6 has-text-grey-light",
            span(hit.doc.fileName),
          ),
          div(
            cls := "level-left",
            p(
              cls := "title is-capitalized is-flex-wrap-wrap",
              a(href := docToLink(hit.doc), target := "_blank", hit.doc.title),
            ),
          ),
          p(cls := "subtitle", hit.doc.body.take(150) + "..."),
        ),
      )
    )

  def renderNoResult: Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "card",
      div(
        cls := "card-content",
        p(cls := "title", "No results"),
        p(cls := "subtitle", "Try a different query."),
      ),
    )

  def renderError(err: String): Resource[IO, HtmlDivElement[IO]] =
    div(
      cls := "card has-background-danger-light",
      div(
        cls := "card-content",
        p(cls := "title", "Error"),
        p(cls := "subtitle has-text-danger", err),
      ),
    )

  def render: Resource[IO, HtmlElement[IO]] = {
    val client = FetchClientBuilder[IO].create

    val fetchDocs: IO[List[Doc]] = client
      .expect[List[Doc]](uri"http4s-docs.json")

    val fetchIndex: IO[MultiIndex] =
      client
        .stream(Request[IO](Method.GET, uri"http4s-docs.idx"))
        .evalMap(r => parseIndexBytes(r.body))
        .compile
        .onlyOrError

    def searchBldr(docs: List[Doc], index: MultiIndex): String => Either[String, List[Hit]] = {
      val qAnalyzer = searchSchema.queryAnalyzer("body")
      val scorer = Scorer(index)
      val allHits = docs.map(r => Hit(r, 0.001))
      qs =>
        if (qs.isEmpty) Right(allHits)
        else {
          val aq = qAnalyzer.parse(qs).map(mq => mq.mapLastTerm(LastTermRewrite.termToPrefix))
          val results: Either[String, List[(Int, Double)]] =
            aq.flatMap(q => index.search(q.qs).flatMap(ds => scorer.score(q.qs, ds.toSet)))
          results.map(hits =>
            hits
              .map((i, score) => Hit(docs(i), score))
              .sortBy(h => -h.score)
          )
        }
    }

    Resource
      .eval((fetchDocs, fetchIndex).parTupled)
      .map(searchBldr.tupled)
      .flatMap(renderList)
      .flatTap(_ => Resource.eval(IO.println("FIN")))
  }

}
