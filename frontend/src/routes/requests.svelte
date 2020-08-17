<script lang="ts">
  import { goto, stores } from '@sapper/app'
  import dayjs from 'dayjs'
  import { onMount } from 'svelte'

  import { getAllPermissions, Permission } from '../api/permissions'
  import { getTasksToReview, Task } from '../api/tasks'
  import Spinner from '../components/Spinner.svelte'

  let permissionsToReview: Permission[] | null = null
  let reviewedPermissions: Permission[] | null = null

  onMount(async () => {
    const [{ given_permissions }, tasks] = await Promise.all([
      getAllPermissions(),
      getTasksToReview(),
    ])
    permissionsToReview = [
      ...given_permissions.filter(
        dr => dr.state === 'pending' || dr.state === 'analyzing'
      ),
      ...tasks.map(t => t.permission),
    ]

    reviewedPermissions = given_permissions.filter(
      dr => dr.state !== 'pending' && dr.state !== 'analyzing'
    )
  })
</script>

<svelte:head>
  <title>Requests</title>
</svelte:head>

<h3 class="display-5">Review incoming requests</h3>

{#if permissionsToReview === null || reviewedPermissions === null}
  <Spinner />
{:else}
  <div class="container-fluid mx-auto mt-2">
    <!-- Not Reviewed Yet Table -->
    <div class="row">
      <div class="col-xl mt-4">
        <h3>
          <small class="text-muted">Not Reviewed Yet</small>
        </h3>
        <div class="table-wrapper-xl">

          <table class="table table-borderless">
            <thead>
              <tr>
                <th class="normal_column" scope="col">Algorithm Owner</th>
                <th class="normal_column" scope="col">Type</th>
                <th scope="col" class="desc_column">Dataset description</th>
                <th class="normal_column" scope="col">Algorithm Name</th>
                <th class="small_column" scope="col">When</th>
                <th class="desc_column" scope="col" />
                <th class="normal_column" scope="col">Action</th>
              </tr>
            </thead>

            <tbody>
              {#each permissionsToReview as request}
                <tr>
                  <td />
                </tr>
                <tr class="rounded-xl">

                  <td class="bg-lightgrey normal_column rounded-xll">
                    {request.algorithm_provider}
                  </td>

                  <td class="bg-lightgrey normal_column">
                    {request.permission_type}
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
                  <td class="bg-lightgrey normal_column" />
                  <td class="bg-lightgrey normal_column rounded-xlr">
                    <button
                      class="btn btn-primary rounded-xl font-weight-bold"
                      on:click={() => goto(`/requests/${request.id}`)}>
                      <div class="px-4">Review</div>
                    </button>
                  </td>

                </tr>
              {:else}
                <tr>
                  <td colspan="6" class="text-center">
                    You have no access requests to review
                  </td>
                </tr>
              {/each}
            </tbody>
            <br />
          </table>
        </div>
      </div>
    </div>

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
                <th class="normal_column" scope="col">Algorithm Owner</th>
                <th class="normal_column" scope="col">Type</th>

                <th scope="col">Dataset description</th>
                <th class="normal_column" scope="col">Algorithm Name</th>
                <th class="small_column" scope="col">When</th>
                <th class="desc_column" scope="col">Folder/Algorithm Run on</th>
                <th class="normal_column" scope="col">Action</th>
              </tr>
            </thead>
            <tbody>
              {#each reviewedPermissions as request}
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
                  <td class="bg-lightgrey normal_column rounded-xlr">
                    <span class="fa-stack fa-2x text-primary">
                      <i class="fas fa-circle fa-stack-2x" />
                      <i class="fas fa-file fa-stack-1x fa-inverse" />
                    </span>
                    {request.dataset || '-'}
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
{/if}
