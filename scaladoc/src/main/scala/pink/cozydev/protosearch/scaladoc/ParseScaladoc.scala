package pink.cozydev.protosearch.scaladoc

import scala.meta._
import scala.meta.contrib._
import scala.meta.contrib.DocToken._
import scala.meta.tokens.Token.Comment
import scala.util.matching.Regex

case class ScaladocInfo(
    name: String,
    description: String,
    annotations: List[String],
    hyperlinks: List[String],
    params: List[String],
    returnType: String,
    startLine: Int,
    endLine: Int,
)

object ParseScaladoc {

  def parseAndExtractInfo(source: String): List[ScaladocInfo] = {
    val parsed: Source = source.parse[Source].get

    val urlRegex: Regex = """https?://[^\s]+""".r

    // Extract comments and their positions
    val comments = parsed.tokens.collect {
      case comment: Comment if comment.syntax.startsWith("/**") =>
        (comment.pos.start, ScaladocParser.parseScaladoc(comment).getOrElse(Nil))
    }.toMap

    val functions = parsed.collect {
      case defn @ Defn.Def.After_4_7_3(_, name, paramss, retType, _) =>
        val (commentTokens, rawComment): (List[DocToken], String) = comments
          .filter { case (start, _) => start < defn.pos.start }
          .toList
          .sortBy(_._1)
          .reverse
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

        val params = paramss.flatMap { case Member.ParamClauseGroup(_, params) =>
          params.flatMap { case clause: Member.ParamClause =>
            clause.values.collect {
              case param: Term.Param
                  if param.default.isEmpty && !param.mods.exists(_.is[Mod.Implicit]) =>
                val commentDescription: String = paramsComm
                  .find(_.startsWith(param.name.value))
                  .getOrElse(param.name.value)
                val declaredType: String = param.decltpe.map(_.toString).getOrElse("Unknown Type")

                s"$commentDescription: $declaredType"
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

        val hyperlinks = urlRegex.findAllMatchIn(rawComment).map(_.matched).toList

        val optionalParams = paramss.flatMap { case Member.ParamClauseGroup(_, params) =>
          params.flatMap { case clause: Member.ParamClause =>
            clause.values.collect {
              case param: Term.Param if param.default.isDefined =>
                val commentDescription: String = paramsComm
                  .find(_.startsWith(param.name.value))
                  .getOrElse(param.name.value)
                val declaredType: String = param.decltpe.map(_.toString).getOrElse("Unknown Type")

                s"$commentDescription: $declaredType"
            }
          }
        }

        val implicitParams = paramss.flatMap { case Member.ParamClauseGroup(_, params) =>
          params.flatMap { case clause: Member.ParamClause =>
            clause.values.collect {
              case param: Term.Param if param.mods.exists(_.is[Mod.Implicit]) =>
                val commentDescription: String = paramsComm
                  .find(_.startsWith(param.name.value))
                  .getOrElse(param.name.value)
                val declaredType: String = param.decltpe.map(_.toString).getOrElse("Unknown Type")

                s"$commentDescription: $declaredType"
            }
          }
        }

        val allParams = typeParams ++ params ++ optionalParams ++ implicitParams

        val returnType = retType match {
          case Some(tpe) => tpe.syntax
          case None => "Unit"
        }

        ScaladocInfo(
          name.value,
          description,
          annotations,
          hyperlinks,
          allParams,
          returnType,
          startLine,
          endLine,
        )
    }
    functions
  }
}
