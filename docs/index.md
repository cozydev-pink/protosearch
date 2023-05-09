## protosearch

### Usage

This library is currently available for Scala binary versions 2.13 and 3.2.

To use the latest version, include the following in your `build.sbt`:

```scala
libraryDependencies ++= Seq(
  "pink.cozydev" %% "protosearch" % "@VERSION@"
)
```

### Demos


## Repo Search

[Repo Search Demo](../reposearch/index.html)

Repo Search is an all in-browser demo, indexing a static JSON file of GitHub repo metadata,
and providing a multi field search over them.


## Search Docs

[Search Docs Demo](../searchdocs/index.html)

The Search Docs demo helps represent indexing server side to produce a `.idx` index file,
requesting and decoding that index browser side, and then search over it.
This is closer to how we imagine search working for most static sites.
Indexing would happen in the JVM as a CI step, and produce a static index file the browser can use.
