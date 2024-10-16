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

package pink.cozydev.protosearch.scaladoc

import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer

object ScaladocIndexer {
  val analyzer = Analyzer.default.withLowerCasing
  val indexBuilder = IndexBuilder.of[ScaladocInfo](
    (Field("functionName", analyzer, stored = true, indexed = true, positions = true), _.name),
    (
      Field("description", analyzer, stored = true, indexed = true, positions = true),
      _.description,
    ),
    (
      Field("params", analyzer, stored = true, indexed = true, positions = true),
      _.params.mkString(", "),
    ),
    (
      Field("annotations", analyzer, stored = true, indexed = true, positions = true),
      _.annotations.mkString(", "),
    ),
    (
      Field("startLine", analyzer, stored = true, indexed = true, positions = true),
      _.startLine.toString(),
    ),
    (
      Field("endLine", analyzer, stored = true, indexed = true, positions = true),
      _.endLine.toString(),
    ),
    (Field("returnType", analyzer, stored = true, indexed = true, positions = true), _.returnType),
  )
}
