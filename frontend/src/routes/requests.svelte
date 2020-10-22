<script lang="ts">
  import { goto, stores } from '@sapper/app'
  import dayjs from 'dayjs'
  import relativeTime from 'dayjs/plugin/relativeTime'
  dayjs.extend(relativeTime)
  import { onMount } from 'svelte'

  import {
    getOutboundPermissions,
    Permission,
    PermissionType,
  } from '../api/permissions'
  import { getTasksToReview, Task } from '../api/tasks'
  import Spinner from '../components/Spinner.svelte'

  type RequestType = PermissionType | 'review output'

  type PendingApproval = {
    from: string
    requestType: RequestType
    description: string
    algorithm: string
    received: Date
    id: number
  }

  let pendingApprovalsP: Promise<PendingApproval[]> = Promise.resolve([])
  let resolvedPermissionsP: Promise<Permission[]> = Promise.resolve([])

  const permissionToRequest = function (
    permission: Permission
  ): PendingApproval {
    return {
      id: permission.id,
      from: permission.algorithm_provider,
      requestType: permission.permission_type,
      description: permission.request_description,
      algorithm: permission.algorithm,
      received: new Date(permission.registered_on),
    }
  }

  const taskToRequest = function (task: Task): PendingApproval {
    return {
      ...permissionToRequest(task.permission),
      requestType: 'review output',
      received: new Date(task.updated_on),
    }
  }

  const permissionApprovableStates = ['pending', 'analyzing']

  onMount(() => {
    const outboundPermissionsP = getOutboundPermissions()

    pendingApprovalsP = Promise.all([
      outboundPermissionsP.then((pp) =>
        pp
          .filter((p) => permissionApprovableStates.indexOf(p.state) > -1)
          .map(permissionToRequest)
      ),
      getTasksToReview().then((tt) => tt.map(taskToRequest)),
    ]).then(([rr1, rr2]) =>
      [...rr1, ...rr2].sort(
        (r1, r2) => r2.received.getTime() - r1.received.getTime()
      )
    )

    resolvedPermissionsP = outboundPermissionsP.then((pp) =>
      pp.filter((p) => permissionApprovableStates.indexOf(p.state) === -1)
    )
  })
</script>

<svelte:head>
  <title>Data Exchange – Requests</title>
</svelte:head>

<h3 class="display-5">Incoming requests</h3>

{#await pendingApprovalsP}
  <Spinner />
{:then incomingRequests}
  <div class="container-fluid mx-auto mt-2">
    <!-- Not Reviewed Yet Table -->
    <div class="row">
    <div class="col-xl mt-4">
      {#if incomingRequests.length > 0}
      <div class="table-wrapper-xl">
        <table class="table table-borderless">
          <thead>
            <tr>
              <th class="normal_column" scope="col">From</th>
              <th class="normal_column" scope="col">Type</th>
              <th scope="col" class="desc_column">Dataset description</th>
              <th class="normal_column" scope="col">Algorithm Name</th>
              <th class="small_column" scope="col">When</th>
              <th class="normal_column" scope="col">Action</th>
            </tr>
          </thead>
          <tbody>
            {#each incomingRequests as request}
              <tr>
                <td />
              </tr>
              <tr class="rounded-xl">

                <td class="bg-lightgrey normal_column rounded-xll">
                  {request.from}
                </td>

                <td class="bg-lightgrey normal_column">
                  {request.requestType}
                </td>

                <td class="bg-lightgrey desc_column">
                  {request.description}
                </td>
                <td class="bg-lightgrey normal_column">
                  {request.algorithm}
                </td>
                <td class="bg-lightgrey normal_column">
                  
                  <span
                  data-toggle="tooltip"
                  title={dayjs(request.received).format('DD-MM-YYYY HH:mm')}>
                  {dayjs(request.received).fromNow()}
                </span>


                </td>
                <td class="bg-lightgrey normal_column rounded-xlr">
                  <button
                    class="btn btn-primary rounded-xl font-weight-bold"
                    on:click={() => goto(`/requests/${request.id}`)}>
                    <div class="px-4">Review</div>
                  </button>
                </td>

              </tr>
            {/each}
          </tbody>
          <br />
        </table>
      </div>        
      {:else}
      <div class="text-center text-muted" role="alert">
        All pending data owner actions will appear here. <br /> <i>The list is empty at the moment.</i>
      </div>
      
      {/if}
    </div>
  </div>
  </div>
{:catch error}
  <p style="color: red">{error.message}</p>
{/await}

{#await resolvedPermissionsP}
  <Spinner />
{:then resolvedPermissions}
  <div class="container-fluid mx-auto mt-2">
    <!-- Reviewed Table -->
    <div class="row mt-5">
    <div class="col">
      <h3>
        <small class="text-muted">Reviewed</small>
      </h3>
      <div class="table-wrapper-xl">
        <table class=" table table-borderless">
          <thead>
            <tr>
              <th class="normal_column" scope="col">From</th>
              <th class="normal_column" scope="col">Type</th>

              <th scope="col">Dataset description</th>
              <th class="normal_column" scope="col">Algorithm Name</th>
              <th class="small_column" scope="col">When</th>
              <th class="desc_column" scope="col">Dataset</th>
              <th class="normal_column" scope="col">Action</th>
            </tr>
          </thead>
          <tbody>
            {#each resolvedPermissions as request}
              <tr>
                <td />
              </tr>
              <tr>
                <td class="bg-lightgrey normal_column rounded-xll">
                  {request.algorithm_provider}
                </td>
                <td class="bg-lightgrey normal_column">
                  {request.permission_type} ({request.state})
                </td>
                <td class="bg-lightgrey desc_column">
                  {request.request_description}
                </td>
                <td class="bg-lightgrey normal_column">
                  {request.algorithm}
                </td>
                <td class="bg-lightgrey normal_column">
                  {dayjs(request.registered_on).format('DD-MM-YYYY HH:mm')}
                </td>
                <td class="bg-lightgrey normal_column rounded-xlr text-center">
                  {#if Boolean(request.dataset)}
                  <span class="fa-stack fa-2x text-primary">
                    <i class="fas fa-circle fa-stack-2x" />
                    <i class="fas fa-file fa-stack-1x fa-inverse" />
                  </span>
                    {request.dataset}
                  
                  {:else}
                    -
                  {/if}
                </td>
                <td class="bg-lightgrey rounded-xlr normal_column">
                  <button
                    class="btn btn-primary rounded-xl font-weight-bold"
                    on:click|preventDefault={() => goto(`/requests/${request.id}`)}>
                    <div class="px-4">Details</div>
                  </button>
                </td>
              </tr>
            {:else}
              <tr>
                <td colspan="6" class="text-center">
                  You have reviewed requests
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    </div>
  </div>
  </div>
{:catch error}
  <p style="color: red">{error.message}</p>
{/await}
