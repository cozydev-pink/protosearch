function render(hit) {
  const path = hit.fields.path
  const htmlPath = hit.fields.path.replace(".txt", ".html")
  const link = new URL("../" + htmlPath, baseUrl)
  const title = hit.fields.title
  const preview = hit.fields.body.slice(0, 150) + "..."
  return (
`
<ol>
  <div class="card">
    <div class="level-left">
      <p class="title">
        <a href="${link}" target="_blank">
          <span>${title}</span>
        </a>
      </p>
    </div>
    <p class="subtitle">${preview}</p>
  </div>
</ol>
`
  )
}

async function main() {
  const modal = document.getElementById("search-modal");
  const modalInput = document.getElementById("search-modal-input");
  const modalBody = document.getElementById("search-modal-content-body");

  const searchTopBar = document.getElementById("search-top-bar")
  searchTopBar.onclick = function() {
    modal.style.display = "block"
    modalInput.focus()
  }

  // When the user clicks on <span> (x), close the modal
  const modalClose = document.getElementsByClassName("search-close")[0];
  modalClose.onclick = function() {
    modal.style.display = "none";
  }
  // When the user clicks anywhere outside of the modal, close it
  window.onclick = function(event) {
    if (event.target == modal) {
      modal.style.display = "none";
    }
  }

  // Setup the search worker, it returns inner html to the modal
  const worker = new Worker(new URL("searchBarWorker.js", baseUrl))
  worker.onmessage = function(e) {
    const markup = e.data.map(render).join("\n")
    modalBody.innerHTML = markup
  }
  // Send inputs to the search worker
  modalInput.addEventListener('input', function () {
    worker.postMessage(this.value)
  })
}

// Only run once page has finished loading
const baseUrl = document.currentScript.src
window.onload = function() {
  main()
}
