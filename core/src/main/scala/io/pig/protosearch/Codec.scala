package io.pig.protosearch

import scodec._
import scodec.bits.BitVector
import cats.effect.IOApp
import cats.effect.IO
import scodec.Attempt.Failure
import scodec.Attempt.Successful

object Codec {
  val nl = BitVector.fromByte('\n')
  val str = codecs.utf8
  val strNl = codecs.vectorDelimited(nl, str)

  val vint = codecs.vint
  val termV = codecs.vectorOfN(vint, vint)
  val vecTermV = codecs.vectorOfN(vint, termV)

  val termIndex = (vecTermV ~ strNl).map { case (vec, terms) =>
    TermIndexArray.unsafeFromVecs(vec, terms)
  }
}
object CodecApp extends IOApp.Simple {

  val xs = Vector("hello", "world")
  val enc = Codec.strNl.encode(xs)
  val dec = enc.flatMap(Codec.strNl.decodeValue)
  val prog = dec match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val termVectors = Vector(
    Vector(1, 2, 3, 4),
    Vector(1, 2, 3, 4),
    Vector(1, 2, 3, 4),
  )
  val decV = Codec.vecTermV.encode(termVectors).flatMap(Codec.vecTermV.decodeValue)
  val decProg = decV match {
    case Failure(cause) => IO.println(s"failed to encode-decode with error: $cause")
    case Successful(value) => IO.println(s"encoded-decoded vector: ${value}")
  }

  val run = prog *> decProg *> IO.println("- FIN -")
}
