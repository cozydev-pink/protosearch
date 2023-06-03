async function getDocs() {
  let docs = fetch("/searchdocs/http4s-docs.json")
    .then(res => res.json())
    .catch((error) => console.error(error));
  return await docs
}

async function getQuerier() {
  let querier = fetch("/searchdocs/http4s-docs.idx")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.load(blob, "body"))
    .catch((error) => console.error(error));
  return await querier
}

function searchIt(docs, querier) {
  var list = ''
  querier.search("client").forEach(h => {
    const title = docs[h.id].title
    const score = parseInt(h.score*1000)
    list += '<li>' + title + ' score: ' + score + '</li>'
  })
  return list
}

async function main() {
  let [docs, querier] = await Promise.all([getDocs(), getQuerier()])
  var app = document.getElementById("app")
  app.innerHTML += searchIt(docs, querier)
}
main()