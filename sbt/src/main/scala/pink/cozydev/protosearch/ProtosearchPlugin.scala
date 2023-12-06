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
import laika.sbt.LaikaPlugin
import laika.sbt.LaikaPlugin.autoImport._

object ProtosearchPlugin extends AutoPlugin {

  override val trigger = allRequirements

  override def requires = plugins.JvmPlugin && LaikaPlugin

  object autoImport {
    val protosearchWriteProtosearchJS = taskKey[Unit]("Write the protosearch.js file")
    val protosearchGenerateIndex = taskKey[Set[File]]("Generate Protosearch Index files")
    val protosearchProcessFiles =
      taskKey[Set[File]]("Process files with Protosearch, don't create final index.")
    val protosearchIndexTarget = settingKey[String]("The target directory for index files")
  }

  import autoImport._

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    protosearchIndexTarget := ((Laika / target).value / "index").toString(),
    protosearchWriteProtosearchJS := Tasks.protosearchWriteProtosearchJS.value,
    protosearchGenerateIndex := Tasks.protosearchGenerateIndex.value,
    protosearchProcessFiles := Tasks.protosearchProcessFiles.value,
  )
}
