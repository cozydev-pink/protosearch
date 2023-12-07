importScripts("./protosearch.js")

async function getQuerier() {
  // TODO aggressive caching
  let querier1 = fetch("./searchIndex.dat")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.load(blob, "body"))
    .catch((error) => console.error("getQuerier error: ", error));
  return await querier1
}

const querierPromise = getQuerier()

async function searchIt(query) {
  const querier = await querierPromise
  console.log("querier", querier)
  var list = ''
  querier.searchPrefix(query).forEach(h => {
    const score = parseInt(h.score*1000)
    list += '<li>' + "title" + ' score: ' + score + '</li>'
  })
  return list
}

onmessage = async function(e) {
  const query = e.data || '' // empty strings become undefined somehow ...
  this.postMessage(await searchIt(query))
}

searchIt("warmup")
