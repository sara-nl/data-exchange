<script lang="ts">
  import Spinner from './Spinner.svelte'
  import { Permission, getAllPermissions } from '../api/permissions'
  import { Task, startWithUserPermisson } from '../api/tasks'
  import { Share } from '../api/shares'
  import { onMount } from 'svelte'
  import { goto, stores } from '@sapper/app'

  // This component shows the form for selecting an algorithm the user has
  // User Permission for and starting a task from it.
  export let algorithms: Share[] | null

  let userPermissions: null | Permission[] = null
  let selectedAlgorithm: null | string = null
  let selectedPermissionId: null | number = null

  onMount(async () => {
    getAllPermissions().then(({ obtained_permissions }) => {
      userPermissions = obtained_permissions.filter(
        p =>
          p.permission_type === 'One specific user permission' &&
          p.state === 'active'
      )
    })
  })

  const runWithPermission = async () => {
    const task = await startWithUserPermisson(
      selectedPermissionId!,
      selectedAlgorithm!
    )
    goto(`/tasks/${task.id}`)
  }
</script>

<!-- Continuous permission runner -->
<div class="col-6 bg-light h-75 rounded">
  <div class="row font-weight-bold px-4 py-4">
    Run an algorithm with continuous permission
  </div>

  <form id="run-permission" on:submit|preventDefault={runWithPermission}>
    <div class="row ml-2 mr-3 w-100">
      <div class="col-lg-3 pl-2">Select algorithm</div>
      <div class="col-lg-9">
        <div class="container">
          {#if algorithms === null}
            <Spinner small />
          {:else if algorithms.length === 0}
            No algorithms available.
          {:else}
            <select
              class="form-control bg-primary text-white rounded select-white
              mr-sm-2"
              id="algorithm-file"
              bind:value={selectedAlgorithm}>
              <option disabled selected="selected" value={null}>
                Select algorithm
              </option>

              {#each algorithms as share}
                <option value={share.path}>{share.path}</option>
              {/each}
            </select>
          {/if}
        </div>
      </div>
    </div>

    <div class="row my-3 ml-2 mr-3 w-100">
      <div class="col-lg-3 pl-2">Select dataset</div>
      <div class="col-lg-9">
        <div class="container">
          {#if userPermissions === null}
            <Spinner small />
          {:else if userPermissions.length === 0}
            No permissions, you need an approved request first.
          {:else}
            <select
              bind:value={selectedPermissionId}
              class="form-control bg-primary text-white rounded select-white
              mr-sm-2"
              id="data-file">
              <option disabled selected="selected" value={null}>
                Select permission
              </option>

              {#each userPermissions as permission}
                <option value={permission.id}>{permission.dataset}</option>
              {/each}
            </select>
          {/if}
        </div>
      </div>
    </div>
    <div class="row my-3 ml-2 mr-3 w-100">
      <div class="col-12">
        <input
          type="submit"
          disabled={selectedPermissionId === null || selectedAlgorithm === null}
          class="btn btn-success"
          form="run-permission"
          value="Run" />
      </div>
    </div>
  </form>
</div>
