# Queries

Protosearch supports queries using boolean logic and a variety of advanced term queries.

## Setup

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

In order to index our domain type `Book`, we'll need a few things:
- An `Analyzer` to convert strings of text into tokens.
- A `SearchSchema` to convert from our `Book` type into multiple fields of text.

And then to create the index itself with a default field:

```scala mdoc:silent
import pink.cozydev.protosearch.{Field, SearchSchema}
import pink.cozydev.protosearch.analysis.Analyzer

val analyzer = Analyzer.default.withLowerCasing
val searchSchema = SearchSchema[Book](
  (Field("author", analyzer, stored=true, indexed=true, positions=false), _.author),
  (Field("title", analyzer, stored=true, indexed=true, positions=true), _.title),
)

val index = searchSchema.indexBldr("title")(books)
```

Finally we'll then need a `search` function to test out.
We use a `queryAnalyzer` with the same default field here to make sure our queries get the same analysis as our documents did at indexing time.


```scala mdoc:silent
val qAnalyzer = searchSchema.queryAnalyzer("title")

def search(q: String): List[Book] =
  qAnalyzer.parse(q)
    .flatMap(mq => index.search(mq.qs))
    .map(hits => hits.map(i => books(i)))
    .fold(_ => Nil, identity)
```

Now we can use our `search` function to explore some different query types!

## Term Queries

The following term queries represent some of the primitive operations on the term dictionary.
In general they specify a way to match one or more terms, and then the query as a whole matches documents containing those terms.

### Term Query

The most basic query is a single term query.
Only documents that contain the term will match.

```scala mdoc
search("fish")
```

### Prefix Query

A prefix query specifies all terms with a given prefix, and then matches all documents containing those terms.

```scala mdoc
search("egg*")
```

### Range Query

A range query similarly specifies a range of terms, and then matches all documents containing those terms.

```scala mdoc
search("[fi TO gz]") // matching 'fish' and 'green'
```

### Phrase Query

A phrase query is made up of one or more terms surrounded by double quotes, and matches documents containing those terms in exactly that order.


```scala mdoc
search("\"red fish, blue fish\"")
```

## Boolean Queries

Boolean queries allow us combine multiple queries together with boolean logic, using the `OR`, `AND`, and `NOT` combinators.


```scala mdoc
search("fish OR ham")
```

```scala mdoc
search("red AND blue")
```

```scala mdoc
search("tale AND NOT mice")
```

## Group Query

As queries get more complex, it can be helpful to group together parts with parenthesis.

```scala mdoc
search("(red OR blue OR green) AND (fish OR mice OR ham)")
```


## Field Query

The field query allows a user to specify the unique field a query should match.
Field queries work with term queries:

```scala mdoc
search("author:suess")
```

```scala mdoc
search("author:beatri*")
```

Additionally, field queries can take more complex boolean queries if specified in a group:

```scala mdoc
search("author:([b TO e] AND NOT dr*)")
```
