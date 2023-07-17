importScripts("./index.js")

async function getDocs() {
  let docs = fetch("../searchdocs/http4s-docs.json")
    .then(res => res.json())
    .catch((error) => console.error(error));
  return await docs
}

async function getQuerier() {
  let querier = fetch("../searchdocs/http4s-docs.idx")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.load(blob, "body"))
    .catch((error) => console.error(error));
  return await querier
}

const docsPromise = getDocs()
const querierPromise = getQuerier()

async function searchIt(query) {
  const docs = await docsPromise
  const querier = await querierPromise
  var list = ''
  querier.searchPrefix(query).forEach(h => {
    const title = docs[h.id].title
    const score = parseInt(h.score*1000)
    list += '<li>' + title + ' score: ' + score + '</li>'
  })
  return list
}

onmessage = async function(e) {
  const query = e.data || '' // empty strings become undefined somehow ...
  this.postMessage(await searchIt(query))
}

searchIt("warmup")
