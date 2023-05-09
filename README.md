# Protosearch

Protosearch is pre-alpha software, do not use in production.

Protosearch is a prototype of a [Lucene][lucene] style search library in pure scala.


## Goals

- Provide building blocks for search on Typelevel sites
- Enable indexing on JVM, searching in browser JS
- Cross compile to JVM / JS / Native
- Support full Lucene query syntax
- Be safe, functional, and performant

## Non Goals

- Competing with or somehow surpassing Lucene
- Being a distributed search like Elasticsearch
- Heavy write workloads


## Lucene Inspired

It's worth calling out how [Lucene][lucene] inspired this library is.
Lucene is an absolutely incredible piece of software.
It has been optimized and extended by a large community for well over 20 years.
If you are looking for very performant search, with a wide range of language support, flexibility and features, you won't find anything better than Lucene on the JVM.

[lucene]: https://lucene.apache.org/
