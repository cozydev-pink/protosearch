const currentScript = document.currentScript
const baseUrl = new URL("../", currentScript.src)

// Read configuration from script data attributes and URL params
function getConfig() {
  const urlParams = new URLSearchParams(location.search)
  return {
    showScore: currentScript?.dataset.showScore === "true",
    showPath: currentScript?.dataset.showPath === "true",
    showPreview: currentScript?.dataset.showPreview !== "false",
    type: urlParams.get("type") || currentScript?.dataset.type,
    query: urlParams.get("q"),
    workerParams: buildWorkerParams(urlParams),
  }
}

function buildWorkerParams(urlParams) {
  const workerParams = new URLSearchParams()
  const index = urlParams.get("index")
  if (index) workerParams.set("index", index)
  return workerParams.toString()
}

function renderHit(hit, config) {
  const path = hit.fields.path.startsWith("/") ? hit.fields.path.slice(1) : hit.fields.path
  const htmlPath = `${path}.html`
  const link = new URL(htmlPath, baseUrl)
  const title = hit.highlights["title"] || hit.fields["title"]
  const preview = hit.highlights["body"]
  const score = hit.score.toFixed(4)

  const previewHtml = config.showPreview && preview
    ? `<p class="ps-preview">${preview}</p>`
    : ""

  const scoreHtml = config.showScore
    ? `<span class="ps-score">score: ${score}</span>`
    : ""

  const pathHtml = config.showPath
    ? `<span class="ps-path">${path}</span>`
    : ""

  const metaHtml = (config.showScore || config.showPath)
    ? `<footer class="ps-meta">${scoreHtml}${pathHtml}</footer>`
    : ""

  return `
<article class="ps-result">
  <header>
    <a href="${link}">${title}</a>
  </header>
  ${previewHtml}
  ${metaHtml}
</article>`
}

function renderScaladoc(hit, config) {
  const title = hit.fields.functionName
  const description = hit.fields.description
  const returnType = hit.fields.returnType
  const params = hit.fields.params

  return `
<article class="ps-result ps-scaladoc">
  <header>${title}</header>
  <p class="ps-preview">${description}</p>
  <dl class="ps-params">
    <dt>Parameters</dt>
    <dd>${params}</dd>
    <dt>Returns</dt>
    <dd>${returnType}</dd>
  </dl>
</article>`
}

function createSearchWorker(config, resultsElement, renderFn) {
  const workerFile = config.workerParams ? `worker.js?${config.workerParams}` : "worker.js"
  const workerUrl = new URL(workerFile, currentScript.src)

  const worker = new Worker(workerUrl)
  worker.onmessage = function(e) {
    const markup = e.data.map(hit => renderFn(hit, config)).join("")
    resultsElement.innerHTML = markup
  }
  return worker
}

function setupModal(config, renderFn) {
  const modal = document.getElementById("search-modal")
  const modalInput = document.getElementById("search-modal-input")
  const modalBody = document.getElementById("search-modal-content-body")
  const searchTopBar = document.getElementById("search-top-bar")

  if (!modal || !modalInput || !modalBody || !searchTopBar) return false

  searchTopBar.onclick = function() {
    modal.style.display = "block"
    modalInput.focus()
  }

  const modalClose = document.getElementsByClassName("search-close")[0]
  if (modalClose) {
    modalClose.onclick = function() {
      modal.style.display = "none"
    }
  }

  window.onclick = function(event) {
    if (event.target == modal) {
      modal.style.display = "none"
    }
  }

  // Keyboard shortcuts: `/` to open, `Escape` to close
  window.addEventListener("keydown", (event) => {
    if (event.defaultPrevented) return
    if (event.code == "Slash" && modal.style.display != "block") {
      event.preventDefault()
      modal.style.display = "block"
      modalInput.focus()
    }
    if (event.code == "Escape" && modal.style.display == "block") {
      event.preventDefault()
      modal.style.display = "none"
    }
  })

  // Send input to worker
  const worker = createSearchWorker(config, modalBody, renderFn)
  modalInput.addEventListener("input", function() {
    worker.postMessage(this.value)
  })

  return true
}

function setupPage(config, renderFn) {
  const resultsContainer = document.getElementById("search-results")
  const searchBar = document.getElementById("search-input")

  if (!resultsContainer || !searchBar) return false

  // Send input to worker
  const worker = createSearchWorker(config, resultsContainer, renderFn)
  searchBar.addEventListener("input", function() {
    worker.postMessage(this.value)
  })

  // If query param `q` is set, use it as initial query
  if (config.query) {
    searchBar.value = config.query
    worker.postMessage(config.query)
  }

  return true
}

function main() {
  const config = getConfig()
  const renderFn = config.type === "scaladoc" ? renderScaladoc : renderHit

  if (!setupModal(config, renderFn)) {
    setupPage(config, renderFn)
  }
}

window.onload = function() {
  main()
}
