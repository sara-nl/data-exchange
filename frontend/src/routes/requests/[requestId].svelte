<script lang="ts">

  	import { onMount } from 'svelte';

    import { getPermission, rejectPermission, headTaskId, approvePermission, Permission } from "../../api/permissions";
    import { spawnTaskFromPermission, Task } from "../../api/tasks";
    import { goto, stores } from "@sapper/app";
    import Spinner from "../../components/Spinner.svelte";
    import TaskInfo from "../../components/TaskInfo.svelte";
    import StepsHeader from "../../components/StepsHeader.svelte";
    import RequestInfo from "../../components/RequestInfo.svelte";
    import RequestActions from "../../components/RequestActions.svelte";
    import { UserRole } from "../../api/users";
    import { mode, token, email } from "../../stores";

    let currentPermission: Permission | null = null
    let currentTask: Task | null = null
    let interactionsAllowed: Boolean = false
    let pageView: UserRole = "algorithm"

    onMount(async () => {
      const { page } = stores();

      page.subscribe(async ({params: {requestId}}) => {
        if (!requestId) {
          return;
        }
        const permission = await getPermission(requestId as number)
        const taskIdOption = await headTaskId(permission.id)
        if (taskIdOption) {
          goto(`/tasks/${taskIdOption.id}`)
        } else {
          currentPermission = permission
          email.subscribe(e => {
          if (permission.dataset_provider === e && permission.algorithm_provider === e) {
              // Let the user decide
              mode.subscribe((m: UserRole) => pageView = m);
          } else if(permission.dataset_provider === e)
              pageView = 'data'
          else
              pageView = 'algorithm'
          });
        }
      })      
    });

    const onRejected = async () => {
      const newPermission = await rejectPermission(currentPermission!.id)
      currentPermission = newPermission
    }

    const onApproved = async (event) => {
      const selectedDataset = event.detail as string
      currentPermission = await approvePermission(currentPermission!.id, selectedDataset)
      if (currentPermission.permission_type === 'stream permission') {
        alert(`Streaming request has been approved. Every new dataset added to ${currentPermission.dataset} will automatically execute the algorithm (unless its code has changed).`)
        goto("/manage_data")
      }
      currentTask = await spawnTaskFromPermission(currentPermission!.id)
      goto(`/tasks/${currentTask.id}`)
    }   
    
    
    const getActiveStep = () => {
      switch(currentPermission.state) {
        case "analyzing":
          return 0
        case "pending":
          return 1
        case "active":
          return 2
        case "rejected":
          return null	
      }
    }

</script>


<svelte:head>
  <title>Request details {currentPermission && currentPermission.id}</title>
</svelte:head>

<div class="row mx-5 my-5 rounded">
    <div class="col-12">
      <button class="btn text-primary" on:click={() => goto(`/requests`)}>
        <i class="fas fa-arrow-left" />
      </button>
      Back
    </div>
</div>
{#if currentPermission === null}
  <Spinner />
{:else}
  <div class="col-10 mx-auto my-5">
      {#if currentPermission.state === 'rejected'}
        <div class="row my-5 mx-auto border border-primary rounded">
          <div class="col-sm text-center text-secondary p-2 font-weight-bold">
            Request rejected by data provider
          </div>
        </div>

        <RequestInfo permission={currentPermission} />
      {:else}
        <StepsHeader activeStep={getActiveStep()} />

        {#if currentPermission.state === 'pending'}
          <RequestInfo permission={currentPermission} />
        {:else}  
          <TaskInfo permission={currentPermission} />
        {/if}
      {/if}

      {#if currentPermission.state === 'pending'}
        {#if pageView === "data"}
          <RequestActions currentPermission={currentPermission} on:rejected={onRejected} on:approved={onApproved}/>
        {:else}
          <h4>Waiting for the data provider to review the permission request</h4>
        {/if}
      {/if}
  </div>
{/if}