async function main() {
  //var app = document.getElementById("app")
  const searchBar = document.getElementById("top-bar-search")
  const modal = document.getElementById("top-bar-search-modal");
  const modalBody = document.getElementById("search-modal-content-body");

  // Get the <span> element that closes the modal
  var span = document.getElementsByClassName("search-close")[0];
  // When the user clicks on <span> (x), close the modal
  span.onclick = function() {
    modal.style.display = "none";
  }
  // When the user clicks anywhere outside of the modal, close it
  window.onclick = function(event) {
    if (event.target == modal) {
      modal.style.display = "none";
    }
  }

  const worker = new Worker("/search/searchBarWorker.js")
  worker.onmessage = function(e) {
    modal.style.display = "block"
    modalBody.innerHTML = e.data
  }

  searchBar.addEventListener('input', function () {
    worker.postMessage(this.value)
  })
}

window.onload = function() {
  main()
}
