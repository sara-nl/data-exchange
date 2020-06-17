<script lang="ts">
  import Spinner from './Spinner.svelte'
  import { Permission } from '../api/permissions'

  // This component shows metrics of the algorithm available after analisys

  export let permission: Permission
  export let analisysDone: Boolean = true
</script>

<div class="row mb-3 font-weight-bold">Algorithm Name</div>
<div class="row mt-1 mb-5">{permission.algorithm}</div>

<div class="row mb-3 font-weight-bold">Algorithm Dependencies</div>
<div class="row mt-1 mb-5">
  {#if analisysDone}
    {#each permission.algorithm_report.imports as dependency}
      <div
        class="col-sm-auto text-center bg-primary text-white rounded mr-1 mt-1">
        {dependency}
      </div>
    {/each}
  {:else}
    <Spinner small />
  {/if}
</div>

<div class="row mb-3 font-weight-bold">Algorithm Length</div>
<div class="row mt-1 mb-5">
  {#if analisysDone}
    Lines: {permission.algorithm_report.lines}, Words: {permission.algorithm_report.words},
    Characters: {permission.algorithm_report.chars}
  {:else}
    <Spinner small />
  {/if}
</div>
