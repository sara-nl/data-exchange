<script lang="ts">
  // XXX Work-around until proper TypeScript support arrives
  declare var $page: any;

  import { onMount } from "svelte";
  import { stores } from "@sapper/app";
  import * as hljs from "highlight.js";

  import LoadFiles from "../../api/loader";
  import Tasks, { TasksReviewRequest } from "../../api/tasks";
  import Spinner from "../../components/Spinner.svelte";

  const { page } = stores();
  const { taskId } = $page.params;

  let state_color = {
    request_rejected: "danger",
    release_rejected: "warning",
    output_released: "success",
    running: "info",
    success: "info",
    error: "danger"
  };

  let visible: boolean = false;
  let ownDatasets: any = null;
  let task: any = null;

  let approve_algorithm_all = false;
  let review_output = true;

  let data = new TasksReviewRequest();

  onMount(async () => {
    await load();
    hljs.initHighlighting();
  });

  async function load() {
    const { data } = await Tasks.retrieve(taskId);
    task = data;

    if (task.state === "data_requested") {
      const { data } = await LoadFiles.start();
      ownDatasets = data.output.own_datasets;
    }
  }

  async function review_request(approved: boolean) {
    data.approved = approved;
    data.updated_request = task;
    data.approve_algorithm_all = approve_algorithm_all;
    data.review_output = review_output;

    try {
      if (approved) {
        task.output = "Running the algorithm";
        task.state = "running";
      }

      let { data: response } = await Tasks.review(taskId, data);
      task.state = response.state;

      if (response.output) {
        task.output = response.output;
      }
    } catch (error) {
      console.log(error.toString());
    }

    // Reload task after reviewing.
    // await load();
  }

  async function release_output(released: boolean) {
    data.released = released;

    try {
      let { data: response } = await Tasks.release(taskId, data);
      task.state = response.state;
    } catch (error) {
      console.log(error.toString());
    }

    // Reload task after reviewing.
    await load();
  }
</script>

<svelte:head>
  <link rel="stylesheet" href="atom-one-light.css" />
  <title>My Files</title>
</svelte:head>

{#if task === null}
  <Spinner />
{:else}
  <h2 class="display-5">
    Request {taskId}
    <small class="text-{state_color[task.state]}">{task.state}</small>
  </h2>

  <div class="container">
    <div class="row">
      <div class="col">
        <div class="my-5">
          <h4>Requester</h4>
          {task.author_email}
          <h4>Data owner</h4>
          {task.approver_email}
        </div>
        <div class="my-5">
          {#if task.state == 'data_requested'}
            <h4>Dataset description</h4>
            {task.dataset_desc}
          {:else}
            <h4>Review output</h4>
            {task.review_output}
          {/if}
        </div>
        <div class="my-5">
          <h4>Algorithm</h4>
          <ul style="list-style:none; padding-left: 0;">
            <li>{task.algorithm}</li>
            <li>
              <button
                class="btn btn-primary"
                on:click={() => (visible = !visible)}>
                {#if visible}Show output{:else}Show algorithm{/if}
              </button>
            </li>
          </ul>
          <h4>Dataset</h4>
          {#if task.is_owner && task.state === 'data_requested'}
            {#if ownDatasets === null}
              <Spinner small />
            {:else if ownDatasets.length === 0}
              No datasets available.
            {:else}
              <select
                class="form-control"
                bind:value={task.dataset}
                id="data-file">
                <option value="">Select dataset</option>

                {#each ownDatasets as file}
                  <option value={file.name}>{file.name}</option>
                {/each}
              </select>
            {/if}
          {:else}{task.dataset || 'No dataset selected'}{/if}
        </div>
      </div>
      <div
        hidden={!visible}
        class="col-12 col-md-8 border"
        style="padding-top: 20px;">
        <h4>{task.algorithm}</h4>
        {#each task.algorithm_content as alg, i}
        <h6>{alg.algorithm_name}</h6>
        <pre>
          <code class="python">
            {alg.algorithm_content || 'Algorithm being processed'}
          </code>
        </pre>
        <h6>{alg.algorithm_info}</h6>
          <hr />
        {/each}
      </div>
      <div
        hidden={visible}
        class="col-12 col-md-8 border"
        style="padding-top: 20px;">
        <pre>{task.output || 'No output (yet)…'}</pre>
      </div>
    </div>

    <div class="row">

      {#if task.state === 'data_requested'}
        {#if task.is_owner}
          <div class="col my-2">
            <h4>Permissions</h4>
            <input bind:checked={approve_algorithm_all} type="checkbox" />
            Approve continous use of dataset with this algorithm.
            <div class="text-muted">
              Only grant this permission if you trust {task.author_email} to
              always run benevolent algorithms.
            </div>
            <input bind:checked={review_output} type="checkbox" />
              Review the output of the algorithm
          </div>
          <div class="col-md-12">
            <button
              disabled={!task.dataset}
              class="btn btn-success"
              on:click={() => review_request(true)}>
              Grant permission
            </button>
            <button
              class="btn btn-danger"
              on:click={() => review_request(false)}>
              Reject
            </button>
          </div>
        {:else}
          <h4>Waiting for the data provider to review the algorithm…</h4>
        {/if}
      {/if}

      {#if task.state === 'success' || (task.state === 'error' && task.review_output) }
        {#if task.is_owner}
          <button class="btn btn-success" on:click={() => release_output(true)}>
            Release Output
          </button>
          <button class="btn btn-danger" on:click={() => release_output(false)}>
            Reject
          </button>
        {:else}
          <h4>Waiting for the data provider to review the output…</h4>
        {/if}
      {/if}
    </div>

  </div>
{/if}
