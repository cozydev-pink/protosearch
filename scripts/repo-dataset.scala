//> using scala "2.13.11"
//> using lib "org.typelevel::cats-effect:3.4.11"
//> using lib "org.http4s::http4s-ember-client:0.23.23"
//> using lib "com.47deg::github4s:0.32.0"
//> using lib "io.circe::circe-core:0.14.3"
//> using lib "co.fs2::fs2-io:3.5.0"

import cats.syntax.all._
import cats.effect.{IO, IOApp}
import fs2.{Stream, text}
import fs2.io.file.{Files, Flags, Path}
import github4s.Github
import github4s.Encoders._
import github4s.domain.{Pagination, Repository}
import io.circe.syntax._
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.client.Client
import scala.concurrent.duration._

object Hello extends IOApp.Simple {

  def gh(c: Client[IO]): IO[Github[IO]] =
    IO.envForIO
      .get("GITHUB_TOKEN")
      .map(Github[IO](c, _))

  def getRepo(gh: Github[IO], ownerName: (String, String)): IO[Repository] =
    IO.sleep(1.second) *>
      gh.repos
        .get(ownerName._1, ownerName._2)
        .flatMap(resp => IO.fromEither(resp.result))

  def getRepos(gh: Github[IO], org: String): IO[List[Repository]] =
    IO.sleep(1.second) *>
      gh.repos
        .listOrgRepos(org, pagination = Some(Pagination(0, 100)))
        .flatMap(resp => IO.fromEither(resp.result))

  def writeRepos(repos: List[Repository]): IO[Unit] =
    Stream
      .emits(repos)
      .map(r => r.asJson.noSpaces)
      .intersperse("\n")
      .through(text.utf8.encode)
      .through(Files[IO].writeAll(Path("repo-dataset.jsonl"), Flags.Append))
      .compile
      .drain

  val orgs = List("typelevel", "http4s", "davenverse", "circe")
  val repos = List(
    ("47degrees", "fetch"),
    ("IronCoreLabs", "cats-scalatest"),
    ("alexarchambault", "argonaut-shapeless"),
    ("alexarchambault", "scalacheck-shapeless"),
    ("alonsodomin", "cron4s"),
    ("armanbilge", "fs2-dom"),
    ("armanbilge", "calico"),
    ("atnos-org", "eff"),
    ("banana-rdf", "banana-rdf"),
    ("bkirwi", "decline"),
    ("erikerlandson", "coulomb"),
    ("etorreborre", "specs2"),
    ("finagle", "finch"),
    ("fthomas", "refined"),
    ("scala-steward", "scala-steward"),
    ("fthomas", "singleton-ops"),
    ("gnieh", "fs2-data"),
    ("http4s", "http4s"),
    ("j-mie6", "parsley-cats"),
    ("janstenpickle", "extruder"),
    ("ltbs", "uniform-scala"),
    ("melrief", "sonic"),
    ("milessabin", "shapeless"),
    ("monix", "monix"),
    ("non", "imp"),
    ("optics-dev", "Monocle"),
    ("outwatch", "outwatch"),
    ("pepegar", "hammock"),
    ("pureconfig", "pureconfig"),
    ("scala-exercises", "scala-exercises"),
    ("scodec", "scodec"),
    ("scoverage", "scalac-scoverage-plugin"),
    ("to-ithaca", "libra"),
    ("tpolecat", "doobie"),
    ("vlovgr", "ciris"),
    ("wheaties", "TwoTails"),
  )

  val run: IO[Unit] = EmberClientBuilder
    .default[IO]
    .build
    .evalMap(c => gh(c))
    .use { ghc =>
      val repoIO = repos.traverse(r => getRepo(ghc, r)).flatMap(writeRepos)
      val orgIO = orgs.traverse_(r => getRepos(ghc, r).flatMap(writeRepos))
      repoIO *> orgIO
    }

}
