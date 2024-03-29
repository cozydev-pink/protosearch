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

import cats.effect.IO
import cats.effect.kernel.Resource
import cats.effect.unsafe.implicits.global
import sbt.*
import laika.io.syntax.*
import laika.io.model.FilePath
import laika.api.Renderer
import pink.cozydev.protosearch.analysis.IndexFormat
import pink.cozydev.protosearch.analysis.Plaintext

object Tasks {
  import Def.*
  import laika.sbt.LaikaPlugin.autoImport._
  import pink.cozydev.protosearch.sbt.ProtosearchPlugin.autoImport._

  val protosearchGenerateIndex: Initialize[Task[Set[File]]] = task {
    val targetDir = FilePath.parse(protosearchIndexTarget.value)
    val outFile = targetDir / "search" / "searchIndex.idx"

    val renderIndex = laika.sbt.Settings.parser.value.use { parser =>
      val tree = Resource.eval(parser.fromInput(laikaInputs.value.delegate).parse)
      val plaintextRenderer = Renderer.of(IndexFormat).withConfig(parser.config).parallel[IO].build
      Resource.both(tree, plaintextRenderer).use { case (tree, renderer) =>
        renderer
          .from(tree)
          .toFile(outFile)
          .render
      }
    }
    val prog = renderIndex <* IO.println(s"rendered to $targetDir")

    val jFile = targetDir.toJavaFile
    if (!jFile.exists()) jFile.mkdirs()
    prog.unsafeRunSync()
    Set(outFile.toJavaFile)
  }

  val protosearchProcessFiles: Initialize[Task[Unit]] = task {
    val userConfig = (Compile / laikaConfig).value
    val targetDir = protosearchIndexTarget.value

    val renderIndex = laika.sbt.Settings.parser.value.use { parser =>
      val tree = Resource.eval(parser.fromInput(laikaInputs.value.delegate).parse)
      val plaintextRenderer = Renderer.of(Plaintext).withConfig(parser.config).parallel[IO].build
      Resource.both(tree, plaintextRenderer).use { case (tree, renderer) =>
        renderer
          .from(tree)
          .toDirectory(FilePath.parse(targetDir))(userConfig.encoding)
          .render
      }
    }
    val prog = renderIndex <* IO.println(s"rendered to $targetDir")

    prog.unsafeRunSync()
  }
}
