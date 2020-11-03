<script lang="ts">
  import AlgorithmReport from './AlgorithmReport.svelte'
  import Spinner from './Spinner.svelte'
  import { UserRole } from '../api/users'
  import { mode, token, email } from '../stores'
  import { onMount } from 'svelte'
  import * as hljs from 'highlight.js'

  import * as jq from 'jquery'

  // @ts-ignore
  export let files: { [fileName: string]: string }

  let expanded: Boolean = false

  onMount(async () => {
    hljs.initHighlighting()
  })

  jq(() =>
    jq('#sourceCode')
      .on('hidden.bs.collapse', () => {
        expanded = false
      })
      .on('shown.bs.collapse', () => {
        expanded = true
      })
  )
</script>

<div class="row mx-auto">
  <p>
    <a
      data-toggle="collapse"
      href="#sourceCode"
      role="button"
      aria-expanded="false"
      aria-controls="sourceCode">
      {#if expanded}Hide{:else}Show{/if}
    </a>
    source code
  </p>
  <div
    class="col-12 collapse border pt-2 h-100 overflow-auto"
    id="sourceCode"
    style="max-height:500px;">
    {#each Object.keys(files) as file}
      <h6>{file}</h6>
      <pre>
        <code class="python">{files[file]}</code>
      </pre>
      <hr />
    {/each}
  </div>
</div>
