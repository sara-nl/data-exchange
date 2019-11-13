<script lang="ts">
  import LoadFiles from "../../api/loader";
  import Tasks from "../../api/tasks";
  import Spinner from "../../components/Spinner.svelte";
  import { goto, stores } from "@sapper/app";
  import { onMount } from "svelte";

  let state_color = {
    request_rejected: "danger",
    release_rejected: "warning",
    output_released: "success",
    running: "info",
    success: "info",
    error: "danger"
  };

  let dataset_tasks = null;
  let alg_tasks = null;

  let datasets = null;
  let algorithms = null;

  onMount(async () => await getUserTasks());

  async function getUserTasks() {
    try {
      let { data: response } = await Tasks.get_logs();
      dataset_tasks = response.data_tasks;
      datasets = Object.keys(dataset_tasks);
      alg_tasks = response.algorithm_tasks;
      algorithms = Object.keys(alg_tasks)
    } catch (error) {
      console.log(error.toString());
    }

    return false;
  }

  function see_details(id: number) {
    goto(`/tasks/${id}`);
  }

  function to_date(datetime: string) {
    let date = new Date(datetime);
    return date.toLocaleDateString();
  }
</script>

<style>
  .table-hover tbody tr {
    cursor: pointer;
  }
</style>

<svelte:head>
  <title>DEX</title>
  <link
    rel="stylesheet"
    href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" />
</svelte:head>

<h2 class="display-5">Logs</h2>

{#if dataset_tasks === null || alg_tasks === null}
  <Spinner />
{:else}
  <div class="container-fluid mx-auto">
    <div class="row">
      <div class="col-xl">
        <h2>
          <small class="text-muted">Tasks with your dataset:</small>
        </h2>
        <table class="table table-responsive table-hover">
          <thead />
          {#each datasets as file, i}
            <tbody>
              <tr
                class="clickable"
                data-toggle="collapse"
                data-target="#data-{i}"
                aria-expanded="false"
                aria-controls="data-{i}">
                <td>
                  {#if file === ""}
                    <b>Dataset not yet specified</b>
                  {:else}
                    <b>{file}</b>
                  {/if}
                </td>
                <td>
                  <i class="fa fa-plus" aria-hidden="true" />
                </td>
              </tr>
            </tbody>
            <thead id="data-{i}" class="collapse">
              <th scope="col">State</th>
              <th scope="col">Requester</th>
              <th scope="col">Algorithm</th>
              <th scope="col">Dataset</th>
              <th scope="col">Permission</th>
              <th scope="col">Registered On</th>
            </thead>
            {#each dataset_tasks[file] as task}
              <tbody id="data-{i}" class="collapse">

                <tr on:click={() => goto(`/tasks/${task.id}`)}>
                  <td>{task.state}</td>
                  <td>{task.author_email}</td>
                  <td>{task.algorithm}</td>
                  <td>{task.dataset}</td>
                  <td>
                    {#if task.permission}
                      <strong>{task.permission.permission_type}</strong>
                    {:else}
                      No
                    {/if}
                  </td>
                  <td>{to_date(task.registered_on)}</td>
                </tr>
              </tbody>
            {/each}
          {/each}
        </table>

      </div>
    </div>
    <div class="row">
      <div class="col-xl">
        <h2>
          <small class="text-muted">Tasks with your algorithm:</small>
        </h2>
        <table class="table table-responsive table-hover">
          <thead />
          {#each algorithms as file, i}
            <tbody>
              <tr
                class="clickable"
                data-toggle="collapse"
                data-target="#alg-{i}"
                aria-expanded="false"
                aria-controls="alg-{i}">
                <td>
                  <b>{file}</b>
                </td>
                <td>
                  <i class="fa fa-plus" aria-hidden="true" />
                </td>
              </tr>
            </tbody>
            <thead id="alg-{i}" class="collapse">
              <th scope="col">State</th>
              <th scope="col">Requester</th>
              <th scope="col">Algorithm</th>
              <th scope="col">Dataset</th>
              <th scope="col">Registered On</th>
            </thead>
            {#each alg_tasks[file] as task}
              <tbody id="alg-{i}" class="collapse">

                <tr on:click={() => goto(`/tasks/${task.id}`)}>
                  <td>{task.state}</td>
                  <td><b>You</b></td>
                  <td>{task.algorithm}</td>
                  <td>{task.dataset}</td>
                  <td>{to_date(task.registered_on)}</td>
                </tr>
              </tbody>
            {/each}
          {/each}
        </table>

      </div>
    </div>
  </div>
{/if}
