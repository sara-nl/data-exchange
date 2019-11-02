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
  let statusList: Array<boolean> = [false, true, true, true, true,
                                    true, true, true, true];

  let data = new TasksReviewRequest();

  onMount(async () => {
    await load_algorithm();
    hljs.initHighlighting();
    await load_dataset();
  });

  function sleep(ms: number) {
    return new Promise(resolve => setTimeout(resolve, ms *  1000));
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
        if (task.state === 'running') {
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
      updated_request: task,
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
{:else}

  <div class="row my-5 border border-primary rounded">
    {#if task.state === 'analyzing_algorithm'}
      <div class="col-sm-12 text-center bg-primary text-white">Waiting for the algorithm analysis..</div>
    {/if}
    {#if task.state === 'data_requested'}
      <div class="col-sm-4 text-center bg-primary text-white">Step 1. Accept algorithm</div>
      <div class="col-sm-4 text-center">Step 2. Running algorithm</div>
      <div class="col-sm-4 text-center">Step 3. Release output</div>
    {/if}
    {#if task.state === 'running'}
      <div class="col-sm-4 text-center text-secondary">Step 1. Accept algorithm</div>
      <div class="col-sm-4 text-center bg-primary text-white">Step 2. Running algorithm</div>
      <div class="col-sm-4 text-center">Step 3. Release output</div>
    {/if}
    {#if task.state === 'success' || (task.state === 'error' && task.review_output) }
      <div class="col-sm-4 text-center text-secondary">Step 1. Accept algorithm</div>
      <div class="col-sm-4 text-center text-secondary">Step 2. Running algorithm</div>
      <div class="col-sm-4 text-center bg-primary text-white">Step 3. Release output</div>
    {/if}
  </div>

  {#if task.state === 'running'}
    <div class="col-sm-12 bg-primary text-white rounded"> Running algorithm</div>
    <div class="Row ml-2"> <Spinner small loading={statusList[0]} text={"Creating container"}/></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[1]} text={"Installing dependencies"}/></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[2]} text={"Downloading data and algorithm to container"}/></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[3]} text={"Blocking all outside access to container"} /></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[4]} text={"Verifying algorithm"} /></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[5]} text={"Running algorithm on data"} /></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[6]} text={"Saving output"} /></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[7]} text={"Deleting container including data and algorithm"} /></div>
    <div class="Row ml-2"> <Spinner small loading={statusList[8]} text={"Wrapping up.."}/></div>


  {:else}
    <div class="row">
      <div class="col-sm-4 h-50">
        <div class="row mb-3 font-weight-bold">Email</div>
        <div class="row mt-1 mb-5">{task.author_email}</div>

        <div class="row mb-3 font-weight-bold">Permission Type</div>
        {#if task.state === 'error' || task.state === 'success'}
          <div class="row mt-1 mb-5">{task.permission.permission_type}</div>
        {:else}
          <div class="row mt-1 mb-5">{"Not available"}</div>
        {/if}

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
                  class="form-control bg-primary text-white rounded custom-select mr-sm-2"
                  bind:value={task.dataset}
                  id="data-file">
                  <option value="">Select dataset</option>

                  {#each ownDatasets as file}
                    <option class="bg-secondary" value={file.name}>{file.name}</option>
                  {/each}
                </select>

              {/if}
            {:else}{task.dataset || 'No dataset selected'}
          {/if}
        </div>
      </div>

      <div class="col-sm-4 h-50">
        <div class="row mb-3 font-weight-bold">Algorithm Name</div>
        <div class="row mt-1 mb-5">{task.algorithm}</div>

        <div class="row mb-3 font-weight-bold">Algorithm Dependencies</div>
        <div class="row mt-1 mb-5">
          {#each task.algorithm_info.algorithm_dependencies as dependency}
            <div class="col-sm-auto text-center bg-primary text-white rounded mr-1 mt-1">{dependency}</div>
          {/each}
        </div>

        <div class="row mb-3 font-weight-bold">Algorithm Length</div>
        <div class="row mt-1 mb-5">
          Newlines: {task.algorithm_info.algorithm_newline},
          Words: {task.algorithm_info.algorithm_words},
          Characters: {task.algorithm_info.algorithm_characters}
        </div>

        <div class="row mb-3 font-weight-bold">Runtime</div>
        {#if task.state === 'error' || task.state === 'success'}
          <div class="row mt-1 mb-5">WIP</div>
        {:else}
          <div class="row mt-1 mb-5 text-warning">Available at step 3</div>
        {/if}
      </div>

      <div class="col-sm-4 pl-0 pr-0" style="height:400px;">
        {#if task.state === 'error' || task.state === 'success'}
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
            <h6>Newlines: {alg.algorithm_newline},
                Words: {alg.algorithm_words},
                Characters: {alg.algorithm_characters}</h6>
              <hr />
            {/each}
          </div>
        {/if}
      </div>
      </div>

      <div class="row">
        {#if task.state === 'data_requested'}
          {#if task.is_owner}
              <button
                disabled={!task.dataset}
                class="btn btn-success mr-3"
                on:click={() => review_request(true)}>
                Run algorithm on data to see output and go to step 2
              </button>
              <button
                class="btn btn-danger"
                on:click={() => review_request(false)}>
                Reject request
              </button>
          {:else}
            <h4>Waiting for the data provider to review the algorithm…</h4>
          {/if}
        {/if}

        {#if task.state === 'success' || (task.state === 'error' && task.review_output) }
          {#if task.is_owner}
            <button class="btn btn-danger mr-3" on:click={() => release_output(false)}>
              Reject request
            </button>
            <button class="btn btn-default text-success" on:click={() => release_output(true)}>
              Release Output
            </button>
          {:else}
            <h4>Waiting for the data provider to review the output…</h4>
          {/if}
        {/if}
      </div>

  {/if}

    <div class="row my-5">OLD BELOW</div>


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

            <div class="form-group">
              <label for="stream">
                <input
                  bind:checked={data.stream}
                  disabled={data.approve_user}
                  id="stream"
                  type="checkbox"
                />

                Automatically run this algorithm on data changes.

                <div class="text-muted">
                  The algorithm will automatically be rerun when changes to
                  your dataset are detected.
              </label>
            </div>

            <div class="form-group">
              <label for="approve_user">
                <input
                  bind:checked={data.approve_user}
                  on:change={() => data.stream = data.stream || data.approve_user}
                  id="approve_user"
                  type="checkbox"
                />

                Approve general use of dataset by the requester.

                <div class="text-muted">
                  With this permission the requester can use any of his algorithms on this
                  dataset. Only grant this permission if you trust {task.author_email} to always
                  run benevolent algorithms.
                </div>
                <div class="text-danger">
                  This option implies the above option.
                </div>
              </label>
            </div>

            <div class="form-group">
              <label for="review_output">
                <input bind:checked={data.review_output} id="review_output" type="checkbox" />
                Review the output of the algorithm
              </label>
            </div>
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
