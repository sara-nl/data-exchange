<script lang="ts">
  // XXX Work-around until proper TypeScript support arrives
  declare var $page: any;

  import { onMount } from "svelte";
  import { goto, stores } from "@sapper/app";
  import * as hljs from "highlight.js";
  import { mode } from "../../stores";

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
  let statusList: Array<boolean> = [
    false,
    true,
    true,
    true,
    true,
    true,
    true,
    true,
    true
  ];

  let data = new TasksReviewRequest();

  onMount(async () => {
    await load_algorithm();
    hljs.initHighlighting();
    await load_dataset();
  });

  function sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms * 1000));
  }

  async function background_processes(second_part: boolean) {
    let from: number = 0;
    let to: number = 5;
    let speed: number = 1;

    if (second_part) {
      from = 5;
      to = 8;
      speed = 3;
    }

    for (let i = from; i < to; i++) {
      let seconds: number = Math.floor(Math.random() * speed + 1);
      await sleep(seconds);
      statusList[i] = false;

      if (i === 7) {
        if (task.state === "running") {
          await sleep(10);
        }
        await load_algorithm();
      }
    }
  }

  async function load_algorithm() {
    const { data } = await Tasks.retrieve(taskId);
    task = data;
  }

  async function load_dataset() {
    if (task.state === "data_requested") {
      const { data } = await LoadFiles.start();
      ownDatasets = data.output.own_datasets;
    }
  }

  async function review_request(approved: boolean) {
    data = {
      ...data,
      approved,
      updated_request: task
    };

    try {
      if (approved) {
        task.output = "Running the algorithm";
        task.state = "running";
        await background_processes(false);
      }

      let { data: response } = await Tasks.review(taskId, data);
      task.state = response.state;

      if (response.output) {
        task.output = response.output;
      }
    } catch (error) {
      console.log(error.toString());
    }
    await background_processes(true);
  }

  async function release_output(released: boolean) {
    data.released = released;

    try {
      let { data: response } = await Tasks.release(taskId, data);
      task.state = response.state;
      load_algorithm();
    } catch (error) {
      console.log(error.toString());
    }
  }
</script>

<svelte:head>
  <title>My Files</title>
</svelte:head>

{#if task === null}
  <Spinner />
{:else if task.state === 'data_requested' && task.permission.permission_type === 'user permission'}
  <div class="row my-5 rounded">
    <div class="col-12">
      <button class="btn text-primary" on:click={() => goto(`/requests`)}>
        <i class="fas fa-arrow-left" />
      </button>
      Back
    </div>
  </div>
  <div>
    <div class="col-10 mx-auto my-5">
      <div class="row my-5 mx-auto rounded-lg text-white bg-primary">
        <div class="col-sm p-2 mx-auto text-center font-weight-bold">
          Give permissions to any algorithm on a specific dataset
        </div>
      </div>
      <div class="row px-3">

        <div class="col-4">
          <div class="row mb-3 font-weight-bold">Algorithm Owner</div>
          <div class="row mt-1 mb-5">{task.author_email}</div>

          <div class="row mb-3 font-weight-bold">Permission Type</div>
          <div class="row mt-1 mb-5">{task.permission.permission_type}</div>
        </div>

        <div class="col-6">

          <div class="row mb-3 font-weight-bold">
            <b>Choose dataset</b>
          </div>
          <div class="row mt-1 mb-5 pr-5">
            {#if task.is_owner && task.state === 'data_requested'}
              {#if ownDatasets === null}
                <Spinner small />
              {:else if ownDatasets.length === 0}
                No datasets available.
              {:else}
                <select
                  class="form-control bg-primary text-white rounded select-white
                  mr-sm-2"
                  bind:value={task.dataset}
                  id="data-file">
                  <option value="">Select dataset</option>

                  {#each ownDatasets as file}
                    <option class="bg-secondary" value={file.name}>
                      {file.name}
                    </option>
                  {/each}
                </select>
            <h6 class="text-muted pt-2">
              With this permission the requester can use any of his algorithms
              on this dataset. Only grant this permission if you trust {task.author_email}
              to always run benevolent algorithms.
            </h6>
              {/if}
            {:else}{task.dataset || 'No dataset selected'}{/if}
          </div>

        </div>
      </div>
      <div class="row">
        {#if task.state === 'data_requested'}
          {#if task.is_owner && $mode === 'data'}
            <button
              disabled={!task.dataset}
              class="btn btn-success rounded-xl px-4 mr-3"
              on:click={() => review_request(true)}>
              Give Permission to run any algorithm on dataset
            </button>
            <button
              class="btn btn-danger rounded-xl px-4"
              on:click={() => review_request(false)}>
              Reject request
            </button>
          {:else}
            <h4>
              Waiting for the data provider to review the permission request
            </h4>
          {/if}
        {/if}
      </div>
    </div>
  </div>
{:else}
  <div class="row my-5 rounded">
    <div class="col-12">
      <button class="btn text-primary" on:click={() => goto(`/requests`)}>
        <i class="fas fa-arrow-left" />
      </button>
      Back
    </div>
  </div>
  <div class="col-10 mx-auto my-5">

    <div class="row my-5 mx-auto border border-primary rounded">
      {#if task.state === 'analyzing_algorithm'}
        <div class="col-sm-12 text-center bg-primary text-white p-2">
          Waiting for the algorithm analysis..
        </div>
      {/if}
      {#if task.state === 'data_requested'}
        <div
          class="col-sm-4 text-center bg-primary text-white p-2 font-weight-bold">
          Step 1. Accept algorithm
        </div>
        <div class="col-sm-4 text-center p-2 font-weight-bold">
          Step 2. Running algorithm
        </div>
        <div class="col-sm-4 text-center p-2 font-weight-bold">
          Step 3. Release output
        </div>
      {/if}
      {#if task.state === 'running'}
        <div class="col-sm-4 text-center text-secondary p-2 font-weight-bold">
          Step 1. Accept algorithm
        </div>
        <div
          class="col-sm-4 text-center bg-primary text-white p-2 font-weight-bold">
          Step 2. Running algorithm
        </div>
        <div class="col-sm-4 text-center p-2 font-weight-bold">
          Step 3. Release output
        </div>
      {/if}
      {#if task.state === 'success' || (task.state === 'error' && task.review_output)}
        <div class="col-sm-4 text-center text-secondary p-2 font-weight-bold">
          Step 1. Accept algorithm
        </div>
        <div class="col-sm-4 text-center text-secondary p-2 font-weight-bold">
          Step 2. Running algorithm
        </div>
        <div
          class="col-sm-4 text-center bg-primary text-white p-2 font-weight-bold">
          Step 3. Release output
        </div>
      {/if}
    </div>

    {#if task.state === 'running'}
      <div class="col-sm-12 bg-primary text-white rounded">
        Running algorithm
      </div>
      <div class="Row ml-2">
        <Spinner small loading={statusList[0]} text="Creating container" />
      </div>
      <div class="Row ml-2">
        <Spinner small loading={statusList[1]} text="Installing dependencies" />
      </div>
      <div class="Row ml-2">
        <Spinner
          small
          loading={statusList[2]}
          text="Downloading data and algorithm to container" />
      </div>
      <div class="Row ml-2">
        <Spinner
          small
          loading={statusList[3]}
          text="Blocking all outside access to container" />
      </div>
      <div class="Row ml-2">
        <Spinner small loading={statusList[4]} text="Verifying algorithm" />
      </div>
      <div class="Row ml-2">
        <Spinner
          small
          loading={statusList[5]}
          text="Running algorithm on data" />
      </div>
      <div class="Row ml-2">
        <Spinner small loading={statusList[6]} text="Saving output" />
      </div>
      <div class="Row ml-2">
        <Spinner
          small
          loading={statusList[7]}
          text="Deleting container including data and algorithm" />
      </div>
      <div class="Row ml-2">
        <Spinner small loading={statusList[8]} text="Wrapping up.." />
      </div>
    {:else}
      <div class="row mx-auto">
        <div class="col-sm-4 h-50">
          <div class="row mb-3 font-weight-bold">Algorithm Owner</div>
          <div class="row mt-1 mb-5">{task.author_email}</div>

          <div class="row mb-3 font-weight-bold">Task state</div>
          <div class="row mt-1 mb-5 text-{state_color[task.state]}">
            {task.state}
          </div>

          <div class="row mb-3 font-weight-bold">Permission Type</div>
          <div class="row mt-1 mb-5">{task.permission.permission_type}</div>

          <div class="row mb-3 font-weight-bold">Permission state</div>
          <div class="row mt-1 mb-5">{task.permission.state}</div>

        </div>

        <div class="col-sm-4 h-50">
          <div class="row mb-3 font-weight-bold">Algorithm Name</div>
          <div class="row mt-1 mb-5">{task.algorithm}</div>

          <div class="row mb-3 font-weight-bold">Algorithm Dependencies</div>
          <div class="row mt-1 mb-5">
            {#if task.algorithm_info.algorithm_dependencies}
              {#each task.algorithm_info.algorithm_dependencies as dependency}
                <div
                  class="col-sm-auto text-center bg-primary text-white rounded
                  mr-1 mt-1">
                  {dependency}
                </div>
              {/each}
            {:else}N/A{/if}
          </div>

          <div class="row mb-3 font-weight-bold">Algorithm Length</div>
          <div class="row mt-1 mb-5">
            Newlines: {task.algorithm_info.algorithm_newline | 'N/A'}, Words: {task.algorithm_info.algorithm_words | 'N/A'},
            Characters: {task.algorithm_info.algorithm_characters | 'N/A'}
          </div>

          {#if task.state === 'error' || task.state === 'success'}
            <div class="row mb-3 font-weight-bold">Used dataset</div>
          {:else}
            <div class="row mb-3 font-weight-bold">Choose dataset</div>
          {/if}
          <div class="row mt-1 mb-5 pr-5">
            {#if task.is_owner && task.state === 'data_requested'}
              {#if ownDatasets === null}
                <Spinner small />
              {:else if ownDatasets.length === 0}
                No datasets available.
              {:else}
                <select
                  class="form-control bg-primary text-white rounded select-white
                  mr-sm-2"
                  bind:value={task.dataset}
                  id="data-file">
                  <option value="">Select dataset</option>

                  {#each ownDatasets as file}
                    <option class="bg-secondary" value={file.name}>
                      {file.name}
                    </option>
                  {/each}
                </select>
              {/if}
            {:else}{task.dataset || 'No dataset selected'}{/if}
          </div>
        </div>

        <div class="col-sm-4 pl-0 pr-0" style="height:400px;">
          {#if task.state === 'error' || task.state === 'success' || task.state === 'output_released' || task.state === 'release_rejected'}
            <div class="row mb-3 font-weight-bold">Output</div>
            <div class="col-12 border pt-2 h-100 overflow-auto">
              <pre>{task.output || 'No output (yet)…'}</pre>
            </div>
          {:else}
            <div class="row mb-3 font-weight-bold">Algorithm Code</div>
            <div class="col-12 border pt-2 h-100 overflow-auto">
              {#each task.algorithm_content as alg}
                <h6>{alg.algorithm_name}</h6>
                <pre>
                  <code class="python">
                    {alg.algorithm_content || 'Algorithm being processed'}
                  </code>
                </pre>
                <h6>
                  Newlines: {alg.algorithm_newline}, Words: {alg.algorithm_words},
                  Characters: {alg.algorithm_characters}
                </h6>
                <hr />
              {/each}
            </div>
          {/if}
        </div>
      </div>

      <div class="row">
        {#if task.state === 'data_requested'}
          {#if task.is_owner && $mode === 'data'}
            <button
              disabled={!task.dataset}
              class="btn btn-success rounded-xl px-4 mr-3"
              on:click={() => review_request(true)}>
              Run algorithm on data to see output and go to step 2
            </button>
            <button
              class="btn btn-danger rounded-xl px-4"
              on:click={() => review_request(false)}>
              Reject request
            </button>
          {:else}
            <h4>Waiting for the data provider to review the algorithm…</h4>
          {/if}
        {/if}

        {#if task.state === 'success' || (task.state === 'error' && task.review_output)}
          {#if task.is_owner && $mode === 'data'}
            <button
              class="btn btn-danger mr-3"
              on:click={() => release_output(false)}>
              Reject request
            </button>
            <button
              class="btn btn-default text-success"
              on:click={() => release_output(true)}>
              Release Output
            </button>
          {:else}
            <h4>Waiting for the data provider to review the output…</h4>
          {/if}
        {/if}
      </div>
    {/if}
  </div>
{/if}
