// Protosearch - Scaladoc Renderer
// Renders search results for Scaladoc API entries
// Fields: functionName, description, params, returnType

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

window.Protosearch.registerRenderer("scaladoc", renderScaladoc)

