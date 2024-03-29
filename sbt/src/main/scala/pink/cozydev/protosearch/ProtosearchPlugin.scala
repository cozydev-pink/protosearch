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
import laika.sbt.LaikaPlugin
import laika.sbt.LaikaPlugin.autoImport._
import laika.helium.Helium

import pink.cozydev.protosearch.ui.SearchUI
import pink.cozydev.protosearch.analysis.IndexRendererConfig

object ProtosearchPlugin extends AutoPlugin {

  override val trigger = allRequirements

  override def requires = plugins.JvmPlugin && LaikaPlugin

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    laikaTheme := laikaTheme.value.extendWith(SearchUI),
    laikaRenderers += IndexRendererConfig(includeInSite = true)
  )
}
