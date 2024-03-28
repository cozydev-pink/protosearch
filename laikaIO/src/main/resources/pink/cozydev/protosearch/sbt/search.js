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
