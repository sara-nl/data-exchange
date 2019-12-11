<script lang="ts">
  import { onMount } from "svelte";
  import dayjs from "dayjs";
  import Permissions, {getObtainerPerFile} from "../api/permissions";

  import Spinner from "../components/Spinner.svelte";
  import File from "../components/File.svelte";
  import State from "../components/State.svelte";

  let permissions: any | null = null;

  const permissionTypes = {
    "user permission": "any algorithm",
    "stream permission": "stream",
  }

  onMount(async () => {
    permissions = await getObtainerPerFile();
  });

</script>

<style>
  .algorithm-row:not(:last-child) {
    border-bottom: 4px solid rgba(0, 0, 0, .2);
  }
</style>

<svelte:head>
  <title>My permissions</title>
</svelte:head>

<h3 class="display-5">My permissions</h3>

<div class="container-fluid mx-auto m-3">

  {#if permissions === null}
    <Spinner />
  {:else}
    {#each Object.entries(permissions) as [file, { permissions, tasks }]}
      <div class="row my-5 p-3 pb-5 border-primary algorithm-row">
        <div class="row w-100">
          <File name={file} />
        </div>
        <div class="row mt-3 w-100">
          <div class="col p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Permissions</small>
            </h3>
            <div>
            {#each permissions.sort((a, b) => a.permission_type < b.permission_type) as permission}
              <div class="permission my-4">
                <File name={`${permission.dataset} (${permissionTypes[permission.permission_type] || permission.permission_type})`} />
              </div>
            {:else}
              No permissions given on this file.
            {/each}
            </div>
          </div>

          <div class="col-1" />
          <div class="col-5 p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Runs</small>
            </h3>
              <div class="table-wrapper">
                <table class="tasks table table-borderless table-sm">
                  <thead>
                    <th class="text-secondary">Dataset</th>
                    <th class="text-secondary">State</th>
                    <th class="text-secondary">Date</th>
                    <th class="text-secondary">Action</th>
                  </thead>
                  <tbody>
                    {#each tasks as task}
                        {#if task.state !== "stream_permission_request"}
                        <tr>
                          <td><File name={task.dataset} /></td>
                          <td><State state={task.state} /></td>
                          <td>{dayjs(task.registered_on).format('DD-MM-YYYY HH:mm')}</td>
                          <td class="font-weight-bold">
                            <a href={`/tasks/${task.id}`}>
                              {#if task.state === "data_requested"}
                                See request
                              {:else if task.state === "output_released"}
                                See output
                              {:else}
                                See details
                              {/if}
                            </a>
                          </td>
                        </tr>
                        {/if}
                    {:else}
                      No tasks for this algorithm.
                    {/each}
                  </tbody>
                </table>
              </div>
          </div>
        </div>
      </div>
    {:else}
      <div>You have not received any permissions</div>
    {/each}
  {/if}

</div>
