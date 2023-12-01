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
import cats.effect.unsafe.implicits.global
import pink.cozydev.protosearch.analysis.DocsDirectory
import sbt.*
import laika.io.model.FilePath

object Tasks {
  import Def.*
  import laika.sbt.LaikaPlugin.autoImport._
  import pink.cozydev.protosearch.sbt.ProtosearchPlugin.autoImport._

  val protosearchGenerateIndex: Initialize[Task[Set[File]]] = task {
    val userConfig = (Compile / laikaConfig).value
    val targetDir = protosearchIndexTarget.value

    val parser = laika.sbt.Settings.parser.value
    val tree = parser.use(_.fromInput(laikaInputs.value.delegate).parse).unsafeRunSync()
    DocsDirectory.plaintextRenderer
      .use(
        _.from(tree)
          .toDirectory(FilePath.parse(targetDir))(userConfig.encoding)
          .render
      )
      .unsafeRunSync()

    val msg = IO.println(s"rendered to ${targetDir}")

    msg.unsafeRunSync()
    Set.empty
  }

}
