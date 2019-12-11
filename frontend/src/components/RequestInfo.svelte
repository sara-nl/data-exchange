<script lang="ts">
  import PermissionInfo from "./PermissionInfo.svelte";
  import AlgorithmReport from "./AlgorithmReport.svelte";
  import Spinner from "./Spinner.svelte";
  import { Permission, permissionInfo } from "../api/permissions";
  import { UserRole } from "../api/users";
  import { mode, token, email } from "../stores";
  import { onMount } from "svelte";
  import * as hljs from "highlight.js";
  
  // @ts-ignore
  export let permission: Permission;

  // TODO: rename algorithm totals to metrics
  let currentMode: UserRole = "algorithm";

  onMount(async () => {
    hljs.initHighlighting();
  });

  email.subscribe(e => {
    if (
      permission.dataset_provider === e &&
      permission.algorithm_provider === e
    ) {
      // Let the user decide
      mode.subscribe(m => (currentMode = m));
    } else if (permission.dataset_provider === e) currentMode = "data";
    else currentMode = "algorithm";
  });

  const analisysAvailable =
    permission.algorithm_report &&
    permission.algorithm_report.totals &&
    permission.algorithm_report.info;
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
    <div class="row mb-3 font-weight-bold">Algorithm eTag</div>
    <div class="row mt-1 mb-5 pr-3">
      {permission.algorithm_etag || '-'}
    </div>
  </div>

  <div class="col-sm-4 h-50">
    <AlgorithmReport permission={permission} analisysDone={analisysAvailable} />
  </div>

  <div class="col-sm-4 pl-0 pr-0" style="height:400px;">
    <div class="row mb-3 font-weight-bold">Algorithm Code</div>
    <div class="col-12 border pt-2 h-100 overflow-auto">
      {#if analisysAvailable}
        {#each permission.algorithm_report.info as alg}
          <h6>{alg.algorithm_name}</h6>
          <pre>
            <code class="python">
              {alg.algorithm_content || 'Algorithm being processed'}
            </code>
          </pre>
          <h6>
            Lines: {alg.algorithm_newline}, Words: {alg.algorithm_words},
            Characters: {alg.algorithm_characters}
          </h6>
          <hr />
        {/each}
      {:else}
        <Spinner small />
      {/if}
    </div>
  </div>
</div>
