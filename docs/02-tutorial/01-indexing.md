Indexing Tutorial
=================

Let's setup a collection of books to search over:

```scala mdoc:silent
case class Book(author: String, title: String)

val books: List[Book] = List(
  Book("Beatrix Potter", "The Tale of Peter Rabbit"),
  Book("Beatrix Potter", "The Tale of Two Bad Mice"),
  Book("Dr. Seuss", "One Fish, Two Fish, Red Fish, Blue Fish"),
  Book("Dr. Seuss", "Green Eggs and Ham"),
)
```

In order to index our domain type `Book`, we'll need a few things:
- An `Analyzer` to convert strings of text into tokens.
- `Field`s to tell the index what kind of data we want to store
- A way to get the values for each of the fields for a given `Book`

We'll pass all these things to an `IndexBuilder`:

```scala mdoc:silent
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer

val analyzer = Analyzer.default.withLowerCasing
val indexBldr = IndexBuilder.of[Book](
  (Field("title", analyzer, stored=true, indexed=true, positions=true), _.title),
  (Field("author", analyzer, stored=true, indexed=true, positions=false), _.author),
)
```

And then we can finally index our `books` using the builder:

```scala mdoc:silent
val index = indexBldr.fromList(books)
```

To learn how to search our `index`, jump over to the [querying tutorial][Querying].