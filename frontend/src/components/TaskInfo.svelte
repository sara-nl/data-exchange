<script lang="ts">
  import PermissionInfo from './PermissionInfo.svelte'
  import AlgorithmReport from './AlgorithmReport.svelte'
  import Spinner from './Spinner.svelte'
  import { Permission, permissionInfo } from '../api/permissions'
  import { UserRole } from '../api/users'
  import { mode, token, email } from '../stores'
  import { onMount } from 'svelte'
  import * as hljs from 'highlight.js'

  // This component fetches all tasks for given permission
  // and shows the detailed status of the most recent one.

  // @ts-ignore
  export let permission: Permission

  // TODO: rename algorithm totals to metrics
  let currentMode: UserRole = 'algorithm'

  onMount(async () => {
    hljs.initHighlighting()
  })

  email.subscribe(e => {
    if (
      permission.dataset_provider === e &&
      permission.algorithm_provider === e
    ) {
      // Let the user decide
      mode.subscribe(m => (currentMode = m || currentMode))
    } else if (permission.dataset_provider === e) currentMode = 'data'
    else currentMode = 'algorithm'
  })

  const reportAvailable = Boolean(permission.algorithm_report)
</script>

<div class="row mx-auto">
  <div class="col-sm-4 h-50">
    <div class="row mb-3 font-weight-bold">Submitted by</div>
    <div class="row mt-1 mb-5">{permission.algorithm_provider}</div>

    <div class="row mb-3 font-weight-bold">Permission Type</div>
    <div class="row mt-1 mb-5">{permission.permission_type}</div>

    <div class="row mb-3 font-weight-bold">Permission Information</div>
    <div class="row mt-1 mb-5 pr-3">
      <PermissionInfo
        permission={permission.permission_type}
        user={currentMode} />
    </div>
    <div class="row mb-3 font-weight-bold">Algorithm hash</div>
    <div class="row mt-1 mb-5 pr-3">{permission.algorithm_etag || '-'}</div>
  </div>

  <div class="col-sm-4 h-50">
    <AlgorithmReport {permission} analisysDone={reportAvailable} />
  </div>

  <div class="col-sm-4 pl-0 pr-0" style="height:400px;">
    <div class="row mb-3 font-weight-bold">Algorithm Code</div>
    <div class="col-12 border pt-2 h-100 overflow-auto">
      {#if reportAvailable}
        {#each Object.keys(permission.algorithm_report.contents) as file}
          <h6>{file}</h6>
          <pre>
            <code class="python">
              {permission.algorithm_report.contents[file]}
            </code>
          </pre>
          <hr />
        {/each}
      {:else}
        <Spinner small />
      {/if}
    </div>
  </div>
</div>
