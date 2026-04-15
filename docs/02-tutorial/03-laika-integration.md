Laika Integration
=================

Protosearch provides two different ways to integrate with [Laika].
There is the base Laika module which defines the `IndexFormat` and `IndexConfig` for building indexes from Laika sites.
And then there is the sbt plugin which assumes use of `TypelevelSitePlugin` which leverages Laika.


IndexFormat
-----------

Each included document in the Laika site is converted to a Protosearch document with three fields:

- *body* - the plaintext content of the Laika document
- *title* - the title of the document
- *path* - the path of the document without file suffix


Users can exclude certain files or file paths by providing an `excludePaths` argument to `IndexFormat`.
This supports both files and directories, and does not require file suffixes.
`IndexFormat` includes a convenience method `.default` which indexes all documents in the Laika site.


IndexConfig
-----------

If you need a Laika `BinaryRendererConfig`, as you might when working with the Laika preview server or Laika sbt plugin, you can use `IndexConfig`.

`IndexConfig` provides two helpers to setup a `IndexConfigBuilder` for you. There is `.default` to index all documents in the Laika site, and `.withExcludedPaths` to exclude some paths.

[Laika]: https://typelevel.org/Laika