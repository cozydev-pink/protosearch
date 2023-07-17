

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
  var app = document.getElementById("app")
  var searchBar = document.getElementById("search_input")

  const worker = new Worker("worker.js")
  worker.onmessage = function(e) {
    app.innerHTML = e.data
  }

  searchBar.addEventListener('input', function () {
    worker.postMessage(this.value)
  })
}
main()
