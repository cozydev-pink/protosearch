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
  const worker = new Worker("/search/searchBarWorker.js")
  worker.onmessage = function(e) {
    modalBody.innerHTML = e.data
  }
  // Send inputs to the search worker
  modalInput.addEventListener('input', function () {
    worker.postMessage(this.value)
  })
}

// Only run once page has finished loading
window.onload = function() {
  main()
}
