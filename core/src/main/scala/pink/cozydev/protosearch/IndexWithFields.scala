package pink.cozydev.protosearch

import cats.data.NonEmptyList
import pink.cozydev.lucille.Query

case class IndexWithFields[A](index: MultiIndex, storedFields: List[A]) {

  def search(q: NonEmptyList[Query]): Either[String, List[A]] =
    index.search(q).map(hs => hs.map(storedFields(_)))

}
object IndexWithFields {
  import scodec.Codec
  import scodec.codecs._

  def codec[A](implicit codecA: Codec[A]): Codec[IndexWithFields[A]] =
    (
      ("MultiIndex" | MultiIndex.codec) ::
        ("list of storedField" | listOfN(vint, variableSizeBytes(vint, codecA)))
    )
      .as[(MultiIndex, List[A])]
      .xmap(
        { case (i, as) => IndexWithFields(i, as) },
        { case (b: IndexWithFields[A]) => (b.index, b.storedFields) },
      )

  val strCodec: Codec[String] = variableSizeBytes(vint, utf8)
}
