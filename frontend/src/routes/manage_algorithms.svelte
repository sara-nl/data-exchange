<script lang="ts">
  import { onMount } from "svelte";
  import dayjs from "dayjs";
  import Permissions from "../api/permissions";

  import Spinner from "../components/Spinner.svelte";
  import File from "../components/File.svelte";
  import State from "../components/State.svelte";

  let permissions: any | null = null;

  onMount(async () => {
    await load();
  });

  async function load() {
    let { data } = await Permissions.get_obtained_per_file();
    permissions = data;
  }
</script>

<style>
  .permission {
    display: inline-block;
  }
</style>

<svelte:head>
  <title>Manage Algorithms</title>
</svelte:head>

<h3 class="display-5">My algorithms & outputs</h3>

<div class="container-fluid m-2">

  {#if permissions === null}
    <Spinner />
  {:else}
    {#each Object.entries(permissions) as [file, { permissions, tasks }]}
      <div class="row my-5 p-4">
        <div class="row w-100">
          <File name={file} />
        </div>
        <div class="row mt-5 w-100">
          <div class="col-4 p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Permissions</small>
            </h3>
            <div>
            {#each permissions as permission}
              <div class="permission my-2">
                <File name={permission.dataset} />
              </div>
            {:else}
              No permissions given on this file.
            {/each}
            </div>
          </div>

          <div class="col-1" />
          <div class="col-7 p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Runs</small>
            </h3>
              <div class="table-wrapper">
                <table class="tasks table table-borderless table-sm">
                  <thead>
                    <th class="text-secondary">Dataset</th>
                    <th class="text-secondary">State</th>
                    <th class="text-secondary">Date</th>
                    <th />
                  </thead>
                  <tbody>
                    {#each tasks as task}
                        {#if task.state !== "stream_permission_request"}
                        <tr>
                          <td><File name={task.dataset} /></td>
                          <td><State state={task.state} /></td>
                          <td>{dayjs(task.registered_on).format("DD-MM-YYYY")}</td>
                          <td><a href={`/tasks/${task.id}`}>Details</a></td>
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
      <div>You have shared no algorithms</div>
    {/each}
  {/if}

</div>
