# Contributing

This guide is for people who would like to be involved in building Protosearch.


## Welcome!

Firstly, welcome and thanks for your interest in Protosearch!


## Getting Started

We hope to maintain a list of [good first issues] to serve as starting points for new contributors.
Occasionally this list might run empty, in which case, feel free to open a new issue asking for suggestions on where to get started.

If you see an issue that interests you, and no one is currently assigned to it, please comment on the issue stating your interest.


## Building The Project

First you'll need to get the code, likely by cloning the repo locally:

```sh
git clone https://github.com/cozydev-pink/protosearch.git
```

Protosearch uses [sbt] and [sbt-typelevel] to manage its build.

Here are some common tasks and their sbt commands:

| Task             | sbt command          |
| --------------   | -------------------- |
| prepare for PR   | `prePR`              |
| format code      | `scalafmtAll`        |
| run JVM tests    | `rootJVM/test`       |
| preview the site | `docs/tlSitePreview` |

The `prePR` command will run formatting and various linting checks similar to what happens when you open a pull request.
It does not run tests though, so be sure to do that as well.

Protosearch cross compiles to multiple platforms, so commands that run for each platform, like `test` can take a while.
It's often nice to focus on a single platform at first, and then test the others.
For example, to run the tests in the core project for just Scala.js run `coreJS/test`.


## Running The Project

Currently the best way to run a modified version of protosearch is to publish the sbt plugin locally.

@:callout(info)
Note that the protosearch sbt plugin only works on projects using [sbt-typelevel]
@:@

There are 3 steps to this process:

Make sure you have started sbt server. If not, enter `sbt` in terminal

### 1. Publish locally
   
1.1. We publish all modules for Scala 2.12 which is what all sbt plugins use.
   Using command:
   
```sh
++2.12.19 publishLocal
```

1.2. Grab the version for our locally published protosearch from the log output.
 
You'll likely see something like the following:

```
delivering :: pink.cozydev#protosearch-sbt;0.0-083dc6f-SNAPSHOT :: 0.0-083dc6f-SNAPSHOT ...
```
It's that `0.0-083dc6f-SNAPSHOT` bit that we want (Note that the version you see will be different).
    
We can use that to add our locally published version of the `protosearch-sbt` plugin to another project's build.

### 2. Add `protosearch-sbt` coords to `plugins.sbt`

Let's add it to [http4s].
    
2.1. Clone http4s, `git clone https://github.com/http4s/http4s.git`
   
2.2. Add the following to its `project/plugins.sbt` file that we got from **step 1**:
    
```
addSbtPlugin("pink.cozydev" % "protosearch-sbt" % "0.0-083dc6f-SNAPSHOT")
```
   
### 3. Run `tlSite`

3.1. Start an sbt session in `http4s` by running `sbt` in your terminal.

3.2. Now we can run `tlSite` for the http4s docs to build an index using our locally published version of protosearch:
    
```sh
site/tlSite
```
Note that sometimes the documentation project is called `docs` and so the command would be `docs/tlSite`.
    
The above will produce `search.html` and `searchIndex.idx` files in the `site/target/docs/site/search/` directory.

3.3. We can use the preview server to run a local web server for the documentation site.

```sh
site/tlSitePreview
```
    

Now, in your local browser, load up [localhost:4242/search/search.html](http://localhost:4242/search/search.html) to see the search UI!


[good first issues]: https://github.com/cozydev-pink/protosearch/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22
[sbt]: https://www.scala-sbt.org/download/
[sbt-typelevel]: https://typelevel.org/sbt-typelevel/
[http4s]: https://http4s.org/
