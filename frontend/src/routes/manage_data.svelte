<script lang="ts">
  import { onMount } from 'svelte'
  import dayjs from 'dayjs'
  import { Share, getShares } from '../api/shares'
  import { StorageNames } from '../api/storage'
  import Tasks from '../api/tasks'
  import Permissions from '../api/permissions'
  import State from '../components/State.svelte'

  import Spinner from '../components/Spinner.svelte'

  let datasetShares: Share[] | null = null
  let data = {}
  let dataset_tasks = {}
  let givenPermissions: { [index: string]: any } = {}

  onMount(async () => {
    datasetShares = await getShares().then((r) => r.own_datasets)
    dataset_tasks = await Tasks.getLogs().then((r) => r.data.data_tasks)

    givenPermissions = await Permissions.getGivenPerFile()
  })

  async function remove_permission(id: string, filename: string) {
    await Permissions.remove(Number(id))

    // TODO: test thoroughly
    givenPermissions = {
      ...givenPermissions,
      [filename]: givenPermissions[filename].filter((p) => p.id !== id),
    }
  }
</script>

<svelte:head>
  <title>Manage Data</title>
</svelte:head>

<h3 class="display-5">Your datasets</h3>

<div class="container-fluid mx-auto m-2">
  {#if datasetShares === null}
    <Spinner />
  {:else}
    {#each datasetShares as share}
      <div class="row my-5 p-4">
        <div class="row">
          <div class="col-auto my-auto">
            <span class="fa-stack fa-2x text-primary">
              <i class="fas fa-circle fa-stack-2x" />
              <i class="fas fa-file fa-stack-1x fa-inverse" />
            </span>
            {share.path}
            ({StorageNames[share.storage]})
          </div>
          <div class="col">
            <button
              class="btn btn-danger rounded-xl font-weight-bold"
              on:click={() => {
                const msg = `You will be redirected to the ${StorageNames[share.storage]} in order to unshare the folder.`
                if (confirm(msg)) window.open(share.webLink, '_blank')
              }}>
              <div class="px-4">Withdraw Data</div>
            </button>
          </div>
        </div>
        <div class="row mt-5 w-100">
          <div class="col-6 mx-5 p-3 rounded-xl background bg-lightgrey">
            <h3><small class="text-muted">Permissions</small></h3>
            {#if givenPermissions !== null && givenPermissions[share.path] !== undefined}
              <div class="table-wrapper">
                <table class="table table-borderless">
                  <thead>
                    <th>With</th>
                    <th>Algorithm</th>
                    <th>Type</th>
                    <th />
                  </thead>
                  <tbody>
                    {#each givenPermissions[share.path] as permission}
                      <tr id={permission['id']} class="my-1">
                        <td>{permission.algorithm_provider}</td>
                        <td>{permission.algorithm}</td>
                        <td>{permission.permission_type}</td>
                        {#if permission.permission_type != 'one time permission'}
                          <td class="text-danger font-weight-bold">
                            <a
                              class="text-danger"
                              href="#0"
                              on:click|preventDefault={() => remove_permission(permission.id, share.path)}>
                              Revoke permission
                            </a>
                          </td>
                        {/if}
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </div>
            {:else}No permissions given on this file{/if}
          </div>
          <div class="col mx-5 p-3 rounded-xl background bg-lightgrey">
            <h3><small class="text-muted">Runs</small></h3>
            {#if dataset_tasks !== undefined && dataset_tasks[share.path] !== undefined}
              <div class="table-wrapper">
                <table class="table table-borderless">
                  <thead>
                    <th>Algorithm Owner</th>
                    <th>Passed</th>
                    <th>When</th>
                    <th>Action</th>
                  </thead>
                  <tbody>
                    {#each dataset_tasks[share.path] as task}
                      {#if task.state !== 'stream_permission_request'}
                        <tr class="my-1">
                          <td>{task.author_email}</td>
                          <td>
                            <State state={task.state} />
                          </td>
                          <td>
                            {dayjs(task.registered_on).format('DD-MM-YYYY HH:mm')}
                          </td>
                          <td class="text-primary font-weight-bold">
                            <a href={`/tasks/${task.id}`}>See log</a>
                          </td>
                        </tr>
                      {/if}
                    {/each}
                  </tbody>
                </table>
              </div>
            {:else}No runs done with this file{/if}
          </div>
        </div>
      </div>
    {:else}
      <div>You have shared no datasetShares</div>
    {/each}
  {/if}
</div>
