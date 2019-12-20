<script lang="ts">
  // XXX Work-around until proper TypeScript support arrives
  declare var $page: any;

  import { onMount } from "svelte";
  import { goto, stores } from "@sapper/app";
  import { mode } from "../../stores";

  import {getShares} from "../../api/shares";
  import Tasks, { Task } from "../../api/tasks";
  import Spinner from "../../components/Spinner.svelte";
  import PermissionInfo from "../../components/PermissionInfo.svelte";
  import AlgorithmReport from "../../components/AlgorithmReport.svelte";
  import TaskProgress from "../../components/TaskProgress.svelte";
  import ErrorMessage from "../../components/ErrorMessage.svelte";
  import StepsHeader from "../../components/StepsHeader.svelte";

  const { page } = stores();
  const { taskId } = $page.params;

  let task: Task | null = null;

  let currentStep: number | null = null;

  onMount(async () => {
    task = await Tasks.retrieve(taskId);
    if (task.state === 'running') {
      currentStep = 1;
      const timerId = setInterval(async () => {
        task = await Tasks.retrieve(taskId);
        currentStep = currentStep + 1
        if(task.state !== 'running') {
          clearInterval(timerId);
          currentStep = 1000; // Makes sure all spinners have stopped
        }
      }, 2000)
    }
  });

  async function release_output(released: boolean) {
    let { data: response } = await Tasks.release(taskId, {released});
    task.state = response.state;
  }
</script>

<svelte:head>
  <title>Task details</title>
</svelte:head>

{#if task === null}
  <Spinner />
{:else}
  <div class="col-10 mx-auto my-5">
      {#if task.state === 'running'}
        <StepsHeader activeStep={2} />
      {/if}
      {#if task.state === 'success' || (task.state === 'error' && task.review_output)}
        <StepsHeader activeStep={3} />
      {/if}

      {#if task.state === 'output_released' || (task.state === 'error' && !task.review_output)}
      <div class="row my-5 mx-auto border border-primary rounded">
        <div class="col-sm text-center text-secondary p-2 font-weight-bold">
          Execution finished
        </div>
      </div>
      {/if}

    {#if (task.state === 'algorithm_changed')}
        <ErrorMessage error={"Execution aborted because algorithm changed"} />
    {/if}    
    {#if (task.state === 'error' && $mode === "data")}
        <ErrorMessage error={"Execution failed with an error"} />
    {/if}

    {#if task.state === 'running'}
      <TaskProgress currentStep={currentStep} />
    {:else}
      <div class="row mx-auto">
        <div class="col-sm-4 h-50">
          {#if $mode === "data"}
            <div class="row mb-3 font-weight-bold">Algorithm Owner</div>
            <div class="row mt-1 mb-5">{task.permission.algorithm_provider}</div>
          {:else}
            <div class="row mb-3 font-weight-bold">Data Owner</div>
            <div class="row mt-1 mb-5">{task.permission.dataset_provider}</div>
          {/if}

          <div class="row mb-3 font-weight-bold">Permission Type</div>
          <div class="row mt-1 mb-5">{task.permission.permission_type}</div>

          <div class="row mb-3 font-weight-bold">Permission Information</div>
          <div class="row mt-1 mb-5 pr-3">
            <PermissionInfo permission={task.permission.permission_type} user={$mode}/>
          </div>
        </div>

        <div class="col-sm-4 h-50">
          <AlgorithmReport permission={task.permission} />  

          <div class="row mb-3 font-weight-bold">Used Dataset</div>
          <div class="row mt-1 mb-5">{task.permission.dataset}</div>        
        </div>

        {#if $mode === "data" || task.state === "output_released"}
        <div class="col-sm-4 pl-0 pr-0" style="height:400px;">
            <div class="row mb-3 font-weight-bold">Output</div>
            <div class="col-12 border pt-2 h-100 overflow-auto">
              <pre>{task.output || 'No output (yet)…'}</pre>
            </div>
        </div>
        {/if}
      </div>

      <div class="row">
        {#if task.state === 'success' || (task.state === 'error' && task.review_output)}
          {#if task.is_owner && $mode === 'data'}
            <button
              class="btn btn-danger mr-3"
              on:click={() => release_output(false)}>
              Reject output
            </button>
            <button
              class="btn btn-default text-success"
              on:click={() => release_output(true)}>
              Release output
            </button>
          {:else}
            <h4>Waiting for the data provider to review the output…</h4>
          {/if}
        {/if}
      </div>
    {/if}
  </div>
{/if}
