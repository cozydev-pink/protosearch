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

function searchIt(query, docs, querier) {
  var list = ''
  querier.searchPrefix(query).forEach(h => {
    const title = docs[h.id].title
    const score = parseInt(h.score*1000)
    list += '<li>' + title + ' score: ' + score + '</li>'
  })
  return list
}

async function main() {
  let [docs, querier] = await Promise.all([getDocs(), getQuerier()])
  var app = document.getElementById("app")
  var searchBar = document.getElementById("search_input")

  searchBar.addEventListener('input', function (evt) {
    app.innerHTML = searchIt(this.value, docs, querier)
  })
}
main()
