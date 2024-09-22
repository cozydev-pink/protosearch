function render(hit) {
  const path = hit.fields.path
  const link = "../" + hit.fields.path.replace(".txt", ".html")
  const title = hit.fields.title
  const preview = hit.fields.body.slice(0, 150) + "..."
  return (
`
<ol>
  <div class="card">
    <div class="card-content">
      <p class="is-size-6 has-text-grey-light">
        <span>${path}</span>
      </p>
      <div class="level-left">
        <p class="title is-capitalized is-flex-wrap-wrap">
          <a href="${link}" target="_blank">
            <span>${title}</span>
          </a>
        </p>
      </div>
      <p class="subtitle">${preview}</p>
    </div>
  </div>
</ol>
`
  )
}

async function main() {
  var app = document.getElementById("app")
  var searchBar = document.getElementById("search_input")
  const urlParams = new URLSearchParams(location.search)
  const maybeIndex = urlParams.get("index")
  const workerJS = maybeIndex ? `worker.js?index=${maybeIndex}` : "worker.js"

  const worker = new Worker(workerJS)
  worker.onmessage = function(e) {
    console.log(e.data)
    const markup = e.data.map(render).join("\n")
    app.innerHTML = markup
  }

  searchBar.addEventListener('input', function () {
    worker.postMessage(this.value)
  })
}
main()
