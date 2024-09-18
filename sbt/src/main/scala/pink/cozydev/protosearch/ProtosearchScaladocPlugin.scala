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
import java.nio.file.Path

object ProtosearchScaladocPlugin extends AutoPlugin {

  lazy val myScalaSourceFiles: TaskKey[Seq[File]] =
    taskKey[Seq[File]]("List all Scala files")

  private def findScalaSourceFiles: Def.Initialize[Task[Seq[File]]] = Def.task {
    val sourceGlobs = (Compile / sourceDirectories).value.map(f => f.toGlob / "**" / "*.scala")
    val scalaSourceFiles: Seq[Path] = sourceGlobs
      .map(g =>
        FileTreeView.default.list(g).collect {
          case (path, attributes) if attributes.isRegularFile => path
        }
      )
      .flatten
    println(s"sourceFiles: ${scalaSourceFiles}")
    scalaSourceFiles.map(_.toFile())
  }
  override val trigger = allRequirements

  override def requires = plugins.JvmPlugin

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    myScalaSourceFiles := findScalaSourceFiles.value
  )
}
