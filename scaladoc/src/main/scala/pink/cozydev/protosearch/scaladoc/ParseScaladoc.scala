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

import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment

case class ScaladocInfo(
    name: String,
    description: String,
    annotations: List[String],
    params: List[String],
    returnType: String,
    startLine: Int,
    endLine: Int,
)

object ParseScaladoc {

  def parseAndExtractInfo(source: String): List[ScaladocInfo] = {
    val parsed: Source = source.parse[Source].get

    // Extract comments and their positions
    val comments = parsed.tokens.collect {
      case comment: Comment if comment.syntax.startsWith("/**") =>
        (comment.pos.start, ScaladocParser.parseScaladoc(comment).getOrElse(Nil))
    }.toMap

    val functions = parsed.collect {
      case defn @ Defn.Def.After_4_7_3(_, name, paramss, retType, _) =>
        val (commentTokens, _): (List[DocToken], String) = comments
          .filter { case (start, _) => start < defn.pos.start }
          .toList
          .sortBy(-_._1)
          .headOption
          .map { case (_, tokens) =>
            val raw = tokens
              .collect { case DocToken(_, _, Some(body)) =>
                body
              }
              .mkString(" ")
            (tokens, raw)
          }
          .getOrElse((Nil, ""))

        val startLine = defn.pos.startLine
        val endLine = defn.pos.endLine

        val description = commentTokens
          .collect { case DocToken(Description, _, Some(body)) =>
            body
          }
          .mkString(" ")

        val paramsComm = commentTokens.collect { case DocToken(Param, Some(name), Some(desc)) =>
          s"$name: $desc"
        }

        val allParams = paramss.flatMap { case Member.ParamClauseGroup(_, params) =>
          params.flatMap { case clause: Member.ParamClause =>
            clause.values.collect { case param: Term.Param =>
              val commentDescription: String = paramsComm
                .find(_.startsWith(param.name.value))
                .getOrElse(param.name.value)
              val declaredType: String = param.decltpe.map(_.toString).getOrElse("Unknown Type")

              val isOptional = param.default.isDefined

              val isImplicit = param.mods.exists(_.is[Mod.Implicit])

              if (isOptional) {
                s"$commentDescription: $declaredType (Optional)"
              } else if (isImplicit) {
                s"$commentDescription: $declaredType (Implicit)"
              } else {
                s"$commentDescription: $declaredType"
              }
            }
          }
        }

        val typeParamsComm = commentTokens.collect {
          case DocToken(TypeParam, Some(name), Some(desc)) => s"@tparam $name: $desc"
        }

        val typeParamRegex = """def\s+\w+\[([^\]]+)\]""".r

        val typeParams = typeParamRegex.findFirstMatchIn(defn.syntax) match {
          case Some(matched) =>
            matched
              .group(1)
              .split(",")
              .map { tparam =>
                val trimmedTparam = tparam.trim
                val commentDescription = typeParamsComm
                  .find(_.startsWith(s"@tparam $trimmedTparam"))
                  .getOrElse(trimmedTparam)
                commentDescription.replace("@tparam" + " " + trimmedTparam, trimmedTparam)
              }
              .toList

          case None => List()
        }

        val annotations = defn.mods.collect { case mod: Mod.Annot =>
          mod.toString
        }

        val combinedParams = typeParams ++ allParams
        val returnType = retType match {
          case Some(tpe) => tpe.syntax
          case None => "Unit"
        }

        ScaladocInfo(
          name.value,
          description,
          annotations,
          combinedParams,
          returnType,
          startLine,
          endLine,
        )
    }
    functions
  }
}
