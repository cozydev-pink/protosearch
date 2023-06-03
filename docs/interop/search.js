var app = document.getElementById("app")
var list = ''

function searchIt(querier) {
  querier.search("client").forEach(h => {
    list += '<li>' + h.id + ' score: ' + h.score + '</li>'
  })
  app.innerHTML += list

}

let querier = fetch("/searchdocs/http4s-docs.idx")
  .then(res => res.blob())
  .then(blob => QuerierBuilder.load(blob, "body"))
  .then(querier => searchIt(querier))
  .catch((error) => console.error(error));