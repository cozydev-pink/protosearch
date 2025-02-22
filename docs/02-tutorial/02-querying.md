Querying
========

We'll quickly setup the same index from the [Indexing Tutorial]:

```scala mdoc:silent
import pink.cozydev.protosearch.{Field, IndexBuilder}
import pink.cozydev.protosearch.analysis.Analyzer

case class Book(author: String, title: String)
val books: List[Book] = List(
  Book("Beatrix Potter", "The Tale of Peter Rabbit"),
  Book("Beatrix Potter", "The Tale of Two Bad Mice"),
  Book("Dr. Seuss", "One Fish, Two Fish, Red Fish, Blue Fish"),
  Book("Dr. Seuss", "Green Eggs and Ham"))

val analyzer = Analyzer.default.withLowerCasing
val index = IndexBuilder.of[Book](
  (Field("title", analyzer, stored=true, indexed=true, positions=true), _.title),
  (Field("author", analyzer, stored=true, indexed=true, positions=false), _.author),
).fromList(books)
```

## Searching

In order to search our index with various queries we need to setup a `SearchInterpreter`.

```scala mdoc:silent
import pink.cozydev.protosearch.SearchInterpreter

val searcher = SearchInterpreter.default(index)
```

And now we can define our search function

```scala mdoc:silent
import pink.cozydev.protosearch.{SearchFailure, SearchRequest, SearchSuccess}

def search(q: String): List[Book] = {
  val req = SearchRequest.default(q)
  searcher.search(req) match {
    case SearchFailure(_) => Nil
    case SearchSuccess(hits) => hits.map(h => books(h.id))
  }
}
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
search("author:seuss")
```

```scala mdoc
search("author:beatri*")
```

Additionally, field queries can take more complex boolean queries if specified in a group:

```scala mdoc
search("author:([b TO e] AND NOT dr*)")
```

## Regex Query

Regex queries allow for greater query flexibility by utilizing powerful regular expressions.

```scala mdoc
search("/jump.*")
```

