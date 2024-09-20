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

package pink.cozydev.protosearch.sbt

import sbt.*
import sbt.Keys.*
import sbt.nio.file.FileTreeView
import fs2.{Stream, Chunk}
import fs2.io.file.{Files, Path}
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all._
import java.nio.file.{Path => JPath}

import pink.cozydev.protosearch.scaladoc.{ParseScaladoc, ScaladocIndexer}
import pink.cozydev.protosearch.MultiIndex

object ProtosearchScaladocPlugin extends AutoPlugin {

  lazy val createScaladocIndex: TaskKey[File] =
    taskKey[File]("Create a Search Index based on Scaladocs within the project")

  private def buildScaladocIndex: Def.Initialize[Task[File]] = Def.task {
    val logger = streams.value.log
    val sourceGlobs = (Compile / sourceDirectories).value.map(f => f.toGlob / "**" / "*.scala")
    val scalaSourceFiles: Seq[JPath] = sourceGlobs.flatMap(g =>
      FileTreeView.default.list(g).collect {
        case (path, attributes) if attributes.isRegularFile => path
      }
    )

    val scaladocInfos = Stream
      .emits(scalaSourceFiles)
      .flatMap { path =>
        val p = Path.fromNioPath(path)
        val content = Files[IO].readAll(p).through(fs2.text.utf8.decode)
        content.map(ParseScaladoc.parseAndExtractInfo)
      }
      .flatMap(Stream.emits)

    val buildIndex = scaladocInfos.compile.toList.map(ScaladocIndexer.createScaladocIndex)

    val projName = name.value
    val targetDir = target.value
    val outputFile = targetDir / s"$projName.idx"
    val writer = Files[IO].writeAll(Path.fromNioPath(outputFile.toPath()))
    val writeIndex = buildIndex.flatMap { index =>
      val numDocs = index.indexes.head._2.numDocs
      val log = IO(logger.info(s"Created index with ${numDocs} docs at $outputFile"))
      val indexBytes = MultiIndex.codec
        .encode(index)
        .map(_.bytes)
        .toEither
        .leftMap(err => new Throwable(err.message))
      val bytes: Stream[IO, Byte] =
        Stream.fromEither[IO](indexBytes).flatMap(bv => Stream.chunk(Chunk.byteVector(bv)))
      log *> bytes.through(writer).compile.drain
    }

    writeIndex.unsafeRunSync()
    outputFile
  }
  override val trigger = allRequirements

  override def requires = plugins.JvmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    createScaladocIndex := buildScaladocIndex.value
  )
}
