importScripts("./protosearch.js")

async function getQuerier() {
  let querier = fetch("./searchIndex.idx")
    .then(res => res.blob())
    .then(blob => QuerierBuilder.load(blob))
    .catch((error) => console.error("getQuerier error: ", error));
  return await querier
}
const querierPromise = getQuerier()

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

async function searchIt(query) {
  const querier = await querierPromise
  return querier.search(query)
    .map(render)
    .join("\n")
}

onmessage = async function(e) {
  const query = e.data || '' // empty strings become undefined somehow ...
  this.postMessage(await searchIt(query))
}

searchIt("warmup")
