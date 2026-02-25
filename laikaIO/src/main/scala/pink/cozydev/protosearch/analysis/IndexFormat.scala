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

package pink.cozydev.protosearch.analysis

import cats.syntax.all.*
import cats.effect.{Async, Resource}
import fs2.{Chunk, Stream}
import laika.api.format.{BinaryPostProcessor, Formatter, TwoPhaseRenderFormat, RenderFormat}
import laika.ast.*
import laika.io.model.{BinaryOutput, RenderedTreeRoot}
import laika.api.builder.OperationConfig
import laika.api.config.{Config, ConfigValue}
import laika.api.config.ConfigValue.*
import laika.theme.Theme
import java.io.OutputStream
import pink.cozydev.protosearch.{Field, IndexBuilder, MultiIndex}
import laika.io.model.RenderedDocument
import laika.api.config.ConfigValue

class IndexFormat(val configKeys: List[String])
    extends TwoPhaseRenderFormat[Formatter, BinaryPostProcessor.Builder] {

  override def description: String = "Protosearch Index"

  val analyzer = Analyzer.default.withLowerCasing

  val baseFields: List[(Field, RenderedDocument => String)] = List(
    (Field("body", analyzer, true, true, true), d => d.content),
    (Field("title", analyzer, true, true, true), d => renderTitle(d.title, d.path)),
    (Field("path", analyzer, true, true, false), d => renderPath(d)),
  )

  val configFields: List[(Field, RenderedDocument => String)] = configKeys.map { key =>
    val field = Field(key, analyzer, true, true, false)
    val getter: RenderedDocument => String = d =>
      d.config
        .getOpt[ConfigValue](key)
        .map {
          case Some(value) => renderConfigValue(value)
          case None => "" // sadly we don't really support optional values yet
        }
        .getOrElse("") // further sadness: we don't really support errors
    (field, getter)
  }

  def renderConfigValue(value: ConfigValue): String = value match {
    case sv: SimpleValue => sv.render
    case ArrayValue(values) => values.map(renderConfigValue).mkString(" ")
    case _ => "" // because we can't return None yet
  }

  val allFields: List[(Field, RenderedDocument => String)] = baseFields ++ configFields

  def interimFormat: RenderFormat[Formatter] = Plaintext

  def prepareTree(tree: DocumentTreeRoot): Either[Throwable, DocumentTreeRoot] =
    Right(tree) // no-op

  /** Post processor that produces the final result based on the interim format.
    */
  def postProcessor: BinaryPostProcessor.Builder = new BinaryPostProcessor.Builder {

    def build[F[_]: Async](config: Config, theme: Theme[F]): Resource[F, BinaryPostProcessor[F]] =
      Resource.pure[F, BinaryPostProcessor[F]](new BinaryPostProcessor[F] {

        def process(
            result: RenderedTreeRoot[F],
            output: BinaryOutput[F],
            config: OperationConfig,
        ): F[Unit] = {
          val docs = result.allDocuments.toList

          val index = IndexBuilder
            .of[RenderedDocument](allFields.head, allFields.tail: _*)
            .fromList(docs)

          val indexBytes = MultiIndex.codec
            .encode(index)
            .map(_.bytes)
            .toEither
            .leftMap(err => new Throwable(err.message))
          val bytes: Stream[F, Byte] = fs2.Stream
            .fromEither(indexBytes)
            .flatMap(bv => Stream.chunk(Chunk.byteVector(bv)))

          val outputStream: Resource[F, OutputStream] = output.resource
          outputStream.use { os =>
            val fos = Async[F].pure(os)
            val pipe = fs2.io.writeOutputStream(fos)
            bytes.through(pipe).compile.drain
          }
        }
      })

  }

  private def renderTitle(title: Option[SpanSequence], path: Path): String =
    title match {
      case Some(span) => span.extractText
      case None => path.name
    }

  private def renderPath(doc: RenderedDocument): String =
    doc.path.withoutSuffix.toString

}

object IndexFormat extends IndexFormat(Nil) {
  def withConfigKeys(keys: List[String]): IndexFormat = new IndexFormat(keys)
}
