## Queries

Protosearch supports queries using boolean logic and a variety of advanced term queries.

Let's setup a collection of books to search over:

```scala mdoc:silent
case class Book(author: String, title: String)

val books: List[Book] = List(
  Book("Beatrix Potter", "The Tale of Peter Rabbit"),
  Book("Beatrix Potter", "The Tale of Two Bad Mice"),
  Book("Dr. Suess", "One Fish, Two Fish, Red Fish, Blue Fish"),
  Book("Dr. Suess", "Green Eggs and Ham"),
)
```

And now for a search function:

@:callout(info)

There's currently a lot of boilerplate involved in this setup which we hope to minimize in future updates.

@:@

```scala mdoc:silent
import pink.cozydev.protosearch.SearchSchema
import pink.cozydev.protosearch.analysis.{Analyzer, QueryAnalyzer}

val analyzer = Analyzer.default.withLowerCasing
val searchSchema = SearchSchema[Book](
  ("author", (b: Book) => b.author, analyzer),
  ("title", (b: Book) => b.title, analyzer),
)

val index = searchSchema.indexBldr("title")(books)
val qAnalyzer = searchSchema.queryAnalyzer("title")

def search(q: String): Either[String, List[Book]] =
  qAnalyzer.parse(q)
    .flatMap(q => index.search(q))
    .map(hits => hits.map(i => books(i)))
```

And finally, a search!

```scala mdoc
search("fish")
```
