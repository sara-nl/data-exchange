<script lang="typescript">
  import { onMount } from 'svelte'
  import dayjs from 'dayjs'
  import Permissions, { getObtainerPerFile } from '../api/permissions'
  import type {
    PermissionType,
    ObtainedPerFile,
    Permission,
  } from '../api/permissions'
  import type { Task } from '../api/tasks'
  import Spinner from '../components/Spinner.svelte'
  import File from '../components/File.svelte'
  import State from '../components/State.svelte'

  const permissionTypes = {
    'One specific user permission': 'any algorithm',
    'stream permission': 'stream',
    'one time permission': 'one time',
  }

  type PermissionsAndTasksPerFile = [
    string,
    {
      permissions: Permission[]
      tasks: (Task & { permission_type: PermissionType })[]
    }
  ][]

  let permissionsP = Promise.resolve<PermissionsAndTasksPerFile>([])

  onMount(() => {
    permissionsP = getObtainerPerFile().then(Object.entries)
  })
</script>

<svelte:head>
  <title>My algorithms</title>
</svelte:head>

<h3 class="display-5">My algorithms</h3>

<div id="algorithms" class="container-fluid mx-auto m-3 accordion">
  {#await permissionsP}
    <Spinner />
  {:then permissions}
    {#each permissions as [file, { permissions, tasks }], i}
      <div class="card">
        <div class="card-header" id="heading{i}">
          <h5 class="mb-0">
            <button
              class="btn btn-link"
              data-toggle="collapse"
              data-target="#collapse{i}"
              aria-expanded="false"
              aria-controls="collapse{i}">
              <span class="fa-stack text-primary">
                <i class="fas fa-circle fa-stack-2x" />
                <i class="fas fa-stack-1x fa-inverse fa-file" />
              </span>
              {file}
            </button>
          </h5>
        </div>
        <div
          id="collapse{i}"
          class="collapse"
          aria-labelledby="heading{i}"
          data-parent="#algorithms">
          <div class="card-body">
            <div class="row w-100">
              <div class="col">
                <h3><small class="text-muted">Active permissions</small></h3>
                {#if permissions.length > 0}
                  <table class="tasks table table-borderless table-sm">
                    <thead>
                      <th class="text-secondary">Dataset</th>
                      <th class="text-secondary">Type</th>
                      <th class="text-secondary">Action</th>
                    </thead>
                    <tbody>
                      {#each permissions.sort((a, b) =>
                        a.permission_type.localeCompare(b.permission_type)
                      ) as permission}
                        <tr>
                          <td>
                            <File name={permission.dataset} folder={false} />
                          </td>
                          <td>{permissionTypes[permission.permission_type]}</td>
                        </tr>
                      {/each}
                    </tbody>
                  </table>
                {:else}
                  <div>No active permissions on this file.</div>
                {/if}
              </div>
            </div>
            <div class="row w-100">
              <div class="col">
                <h3><small class="text-muted">Runs</small></h3>
                {#if tasks.length > 0}
                  <div class="table-wrapper">
                    <table class="tasks table table-borderless table-sm">
                      <thead>
                        <th class="text-secondary">Dataset</th>
                        <th class="text-secondary">Permission</th>
                        <th class="text-secondary">State</th>
                        <th class="text-secondary">Date</th>
                        <th class="text-secondary">Action</th>
                      </thead>
                      <tbody>
                        {#each tasks as task}
                          <tr>
                            <td>
                              <File name={task.dataset} />
                            </td>
                            <td>{permissionTypes[task.permission_type]}</td>
                            <td>
                              <State state={task.state} />
                            </td>
                            <td>
                              {dayjs(task.registered_on).format('DD-MM-YYYY HH:mm')}
                            </td>
                            <td class="font-weight-bold">
                              <a href={`/tasks/${task.id}`}>
                                {#if task.state === 'output_released'}
                                  See output
                                {:else}See details{/if}
                              </a>
                            </td>
                          </tr>
                        {/each}
                      </tbody>
                    </table>
                  </div>
                {:else}
                  <div>No runs on this algorithm yet</div>
                {/if}
              </div>
            </div>
          </div>
        </div>
      </div>
    {:else}
      <div>You have not uploaded any algorithms yet</div>
    {/each}
  {:catch error}
    <p style="color: red">{error.message}</p>
  {/await}
</div>
