importScripts("./protosearch.js")

async function getQuerier() {
  let querier = fetch("./searchIndex.dat")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.loadIndexWithFields(blob, "body"))
    .catch((error) => console.error("getQuerier error: ", error));
  return await querier
}

const querierPromise = getQuerier()

async function searchIt(query) {
  const querier = await querierPromise
  var list = ''
  querier.search(query)
    .sort((h1, h2) => h1.score < h2.score)
    .forEach(h => {
      const score = parseInt(h.score*1000)
      const preview = h.body.slice(0, 80) + "..."
      list += '<li> id:' + h.id + ' score: ' + score + ' body: ' + preview + '</li>'
    })
  return list
}

onmessage = async function(e) {
  const query = e.data || '' // empty strings become undefined somehow ...
  this.postMessage(await searchIt(query))
}

searchIt("warmup")
