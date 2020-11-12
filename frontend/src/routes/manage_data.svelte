<script lang="typescript">
  import { onMount } from 'svelte'
  import dayjs from 'dayjs'
  import { Share, getShares } from '../api/shares'
  import { StorageNames } from '../api/storage'
  import Tasks from '../api/tasks'
  import type { Task } from '../api/tasks'
  import Permissions, {
    Permission,
    permissionTypesShortLabels,
  } from '../api/permissions'
  import State from '../components/State.svelte'

  import Spinner from '../components/Spinner.svelte'
  import File from '../components/File.svelte'

  let dataInfoP = Promise.resolve<
    [Share[], { [path: string]: Task[] }, { [path: string]: Permission[] }]
  >([[], {}, {}])

  const loadData = async () => {
    dataInfoP = Promise.all([
      getShares().then((r) => r.own_datasets),
      Tasks.getLogs().then((r) => r.data_tasks),
      Permissions.getGivenPerFile(),
    ])
  }

  onMount(loadData)

  const revokePermission = async (id: number) => {
    await Permissions.remove(id)
    await loadData()
  }
</script>

<svelte:head>
  <title>Manage Data</title>
</svelte:head>

<h3 class="display-5">My datasets</h3>

<div class="container-fluid mx-auto m-3 accordion">
  {#await dataInfoP}
    <Spinner />
  {:then [shares, tasks, permissions]}
    <div class={shares.length > 1 ? 'accordion' : ''}>
      {#each shares as s, i}
        <div class="card">
          <div class="card-header" id="heading{i}">
            <div class="row">
              <div class="col-3">
                <button
                  class="btn btn-link"
                  data-toggle="collapse"
                  data-target="#collapse{i}"
                  aria-expanded="false"
                  aria-controls="collapse{i}">
                  <File name={s.path} folder={s.isDirectory} />
                </button>
              </div>
              <div class="col-3 small">
                <div class="row">
                  <span class="text-muted">Storage:</span>
                  &nbsp;
                  {StorageNames[s.storage]}
                </div>
                <div class="row">
                  {(permissions[s.path] || []).length}
                  &nbsp;
                  <span class="text-muted">permissions |</span>
                  &nbsp;
                  {(tasks[s.path] || []).length}
                  &nbsp;
                  <span class="text-muted">runs</span>
                </div>
              </div>
              <div class="col-3">
                <a
                  class="text-danger font-weight-bold"
                  href="#0"
                  on:click|preventDefault={() => {
                    const msg = `You will be redirected to the ${StorageNames[s.storage]} in order to unshare the folder.`
                    if (confirm(msg)) window.open(s.webLink, '_blank')
                  }}>
                  Withdraw
                </a>
              </div>
            </div>
          </div>
          <div id="collapse{i}" class="collapse" aria-labelledby="heading{i}">
            <div class="card-body">
              <div class="row w-100">
                <div class="col">
                  <h3><small class="text-muted">Active permissions</small></h3>
                  {#if permissions[s.path] !== undefined}
                    <table class="table table-borderless">
                      <thead>
                        <th>With</th>
                        <th>Algorithm</th>
                        <th>Type</th>
                        <th />
                      </thead>
                      <tbody>
                        {#each permissions[s.path] as permission}
                          <tr>
                            <td>{permission.algorithm_provider}</td>
                            <td>{permission.algorithm}</td>
                            <td>
                              {permissionTypesShortLabels[permission.permission_type]}
                            </td>

                            <td class="text-danger font-weight-bold">
                              <a
                                class="text-danger"
                                href="#0"
                                role="button"
                                on:click|preventDefault={() => revokePermission(permission.id)}>
                                Revoke
                              </a>
                            </td>
                          </tr>
                        {/each}
                      </tbody>
                    </table>
                  {:else}No permissions given on this file{/if}
                </div>
              </div>
              <div class="row w-100">
                <div class="col">
                  <h3><small class="text-muted">Runs</small></h3>
                  {#if tasks[s.path] !== undefined}
                    <table class="table table-borderless">
                      <thead>
                        <th>Permission</th>
                        <th>Algorithm Owner</th>
                        <th>Status</th>
                        <th>When</th>
                        <th>Action</th>
                      </thead>
                      <tbody>
                        {#each tasks[s.path] as task}
                          <tr class="my-1">
                            <td>
                              {permissionTypesShortLabels[task.permission.permission_type]}
                            </td>
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
                        {/each}
                      </tbody>
                    </table>
                  {:else}No runs done with this file{/if}
                </div>
              </div>
            </div>
          </div>
        </div>
      {:else}
        <div>You have not shared any datasets</div>
      {/each}
    </div>
  {:catch error}
    <p style="color: red">{error.message}</p>
  {/await}
</div>
