<script lang="ts">
  import PermissionInfo from './PermissionInfo.svelte'
  import Spinner from './Spinner.svelte'
  import {
    Permission,
    permissionApprovalActionDict,
    permissionInfo,
  } from '../api/permissions'
  import { UserRole } from '../api/users'
  import { mode, token, email } from '../stores'
  import { getShares, Share } from '../api/shares'
  import { createEventDispatcher } from 'svelte'
  import { StorageNames } from '../api/storage'

  export let currentPermission: Permission

  let availableDatasets: Share[] | null = null
  let selectedDataset: Share | null = null

  getShares().then(({ own_datasets }) => {
    if (currentPermission.permission_type === 'stream permission') {
      availableDatasets = own_datasets.filter((ds) => ds.isDirectory)
    } else {
      availableDatasets = own_datasets
    }
  })
  const dispatch = createEventDispatcher()
</script>

<div class="row mb-3">
  <div class="col-sm-4">
    {#if currentPermission.permission_type === 'stream permission'}
      <b>Choose stream of datasets</b>
    {:else}<b>Choose dataset:</b>{/if}

    {#if availableDatasets === null}
      <Spinner small />
    {:else if availableDatasets.length === 0}
      No datasets available.
    {:else}
      <select
        class="form-control bg-primary text-white rounded select-white mr-sm-2"
        bind:value={selectedDataset}
        id="data-file">
        <option value="">Select dataset</option>

        {#each availableDatasets as share}
          <option class="bg-secondary" value={share}>
            {share.path}
            ({StorageNames[share.storage]})
          </option>
        {/each}
      </select>
    {/if}
  </div>
</div>

<div class="row mb-3">
  <div class="col-sm-8">
    <b>Please read this carefully!</b>
    <br />
    {permissionInfo(currentPermission.permission_type, $mode)}
  </div>
</div>
<div class="row mb-3 font-weight-bold">
  <button
    disabled={!selectedDataset}
    class="btn btn-success rounded-xl px-4 mr-3"
    on:click|preventDefault={() => dispatch('approved', selectedDataset)}>
    {permissionApprovalActionDict[currentPermission.permission_type]}
  </button>
  <button
    class="btn btn-danger rounded-xl px-4"
    on:click|preventDefault={() => dispatch('rejected')}>
    Reject request
  </button>
</div>
