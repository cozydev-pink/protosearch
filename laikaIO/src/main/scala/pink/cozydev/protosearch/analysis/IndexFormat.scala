package pink.cozydev.protosearch.analysis

import cats.effect.{Async, Resource}
import fs2.Stream
import laika.api.format.{BinaryPostProcessor, Formatter, TwoPhaseRenderFormat, RenderFormat}
import laika.ast.*
import laika.io.model.{BinaryOutput, RenderedTreeRoot}
import laika.api.builder.OperationConfig
import laika.api.config.Config
import laika.theme.Theme
import java.io.OutputStream

case object IndexFormat extends TwoPhaseRenderFormat[Formatter, BinaryPostProcessor.Builder] {

  override def description: String = "Protosearch Index"

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
          val strs: List[String] = result.allDocuments.map(_.content).toList
          val ss: Stream[F, Byte] = fs2.Stream.emits[F, String](strs).map(_.toByte)

          val bytes: Stream[F, Byte] = ss
          val outputStream: Resource[F, OutputStream] = output.resource
          outputStream.use { os =>
            val fos = Async[F].pure(os)
            val pipe = fs2.io.writeOutputStream(fos)
            bytes.through(pipe).compile.drain
          }
        }
      })

  }

}
