<script lang="typescript">
  import { onMount } from 'svelte'
  import dayjs from 'dayjs'

  import { getShares } from '../../api/shares'
  import type { Share } from '../../api/shares'
  import { StorageNames } from '../../api/storage'
  import {
    permissionTypesShortLabels,
    permissionTypeLabels,
    requestPermission,
    getAllPermissions,
    PermissionRequest,
    Permission,
  } from '../../api/permissions'
  import Spinner from '../../components/Spinner.svelte'
  import PermissionInfo from '../../components/PermissionInfo.svelte'
  import ErrorMessage from '../../components/ErrorMessage.svelte'

  let algorithmShares: Share[] | null = null
  let requesting = false
  let showError: any = null

  let newDataRequest: Partial<PermissionRequest> = {}
  let pendingRequests: Permission[] | null = null

  onMount(async () => {
    algorithmShares = await getShares().then((r) => r.own_algorithms)

    getAllPermissions().then(({ inbound }) => {
      pendingRequests = inbound
        .filter((r) => r.state === 'pending' || r.state === 'analyzing')
        .sort((a, b) => b.id - a.id) // newer first
    })
  })

  async function createRequest(event: any) {
    requesting = true
    newDataRequest.algorithm = newDataRequest.algorithm_share.path
    newDataRequest.algorithm_storage = newDataRequest.algorithm_share.storage
    delete newDataRequest.algorithm_share
    requestPermission(newDataRequest as PermissionRequest)
      .then((storedRequest) => {
        requesting = false
        pendingRequests =
          pendingRequests === null
            ? [storedRequest]
            : [storedRequest, ...pendingRequests]
        clearForm()
      })
      .catch((error) => {
        requesting = false
        clearForm()
        showError = (error.response && error.response.data) || null
      })
  }

  function clearForm() {
    ;(<HTMLFormElement>document.getElementById('request-permission')).reset()
    ;(<HTMLInputElement>document.getElementById('permissions')).value = ''
    ;(<HTMLInputElement>document.getElementById('algorithm-file')).value = ''
    ;(<HTMLInputElement>document.getElementById('data_owner')).value = ''
    ;(<HTMLInputElement>document.getElementById('dataset_desc')).value = ''
  }
</script>

<svelte:head>
  <title>Create request</title>
</svelte:head>

<ErrorMessage error={showError} />
<div class="container-fluid mx-auto">
  <div class="row justify-content-center">
    <div class="col-8 bg-primary text-white rounded">
      <!-- Request permission -->
      <form id="request-permission" on:submit|preventDefault={createRequest}>
        <div class="row ml-1 font-weight-bold px-3 py-4">
          Request Permission for a dataset
        </div>

        <div class="row mb-3 ml-2 mr-3">
          <div class="col-3 pl-2 w-100">Type of permission</div>
          <div class="col-9">
            <div class="container w-100">
              <select
                class="form-control bg-light text-dark custom-select rounded
                mr-sm-2"
                id="permissions"
                bind:value={newDataRequest.permission_type}>
                <option selected disabled value="">
                  No permission selected
                </option>

                {#each Object.keys(permissionTypeLabels) as permissionType}
                  <option value={permissionType}>
                    {permissionTypeLabels[permissionType]}
                  </option>
                {/each}
              </select>
            </div>
          </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
          <div class="col-lg-3 pl-2" />
          <div class="col-9">
            <div class="container my-0 py-0 mx-0 my-0">
              {#if newDataRequest.permission_type}
                <PermissionInfo permission={newDataRequest.permission_type} />
              {:else}
                You have to select a specific permission in order to make a
                request to a dataowner.
              {/if}
            </div>
          </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
          <div class="col-lg-3 pl-2">Select algorithm</div>
          <div class="col-lg-9">
            <div class="container">
              {#if algorithmShares === null}
                <Spinner small />
              {:else if algorithmShares.length === 0}
                No algorithms available.
              {:else}
                <select
                  class="form-control bg-light text-black custom-select rounded
                  mr-sm-2"
                  id="algorithm-file"
                  bind:value={newDataRequest.algorithm_share}>
                  <option disabled selected="selected" value="">
                    Select algorithm
                  </option>

                  {#each algorithmShares as share}
                    <option value={share}>
                      {share.path} ({StorageNames[share.storage]})
                    </option>
                  {/each}
                </select>
              {/if}
            </div>
          </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
          <div class="col-3 pl-2">Data owner email</div>
          <div class="col-9">
            <div class="container">
              <input
                class="form-control"
                type="text"
                id="data_owner"
                bind:value={newDataRequest.dataset_provider} />
            </div>
          </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
          <div class="col-3 pl-2">Dataset description</div>
          <div class="col-9">
            <div class="container">
              <textarea
                rows="5"
                class="form-control"
                id="dataset_desc"
                bind:value={newDataRequest.request_description} />
            </div>
          </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
          <div class="col-12">
            <input
              type="submit"
              disabled={!(newDataRequest.algorithm_share && newDataRequest.dataset_provider && newDataRequest.request_description && newDataRequest.permission_type) || requesting}
              class="btn btn-success"
              form="request-permission"
              value={requesting ? 'Requesting...' : 'Request'} />
          </div>
        </div>
      </form>
    </div>
  </div>
  <div class="my-5">
    <h3 class="display5">Pending Requests</h3>
  </div>
  <!-- Running Requests -->
  <div class="row justify-content-center py-3 bg-light rounded">
    <div class="col">
      {#if pendingRequests === null || pendingRequests.length === 0}
        <div class="row px-4 ml-1">
          Currently there are no pending requests.
        </div>
      {:else}
      <div class="row px-4 mb-2">
        <div class="col-2 font-weight-bold">Registered</div>
        <div class="col-3 font-weight-bold">Reviewer</div>
        <div class="col-2 font-weight-bold">Type</div>
        <div class="col-5 font-weight-bold">Description</div>
      </div>
        {#each pendingRequests as pendingRequest}
          <div class="row px-4 mb-1">
            <div class="col-2">
              {dayjs(pendingRequest.registered_on).format('DD-MM-YYYY HH:mm')}
            </div>
            <div class="col-3">
              {pendingRequest.dataset_provider}
              <br />
              <a href="/requests/{pendingRequest.id}">View</a>
            </div>
            <div class="col-2">
              {permissionTypesShortLabels[pendingRequest.permission_type]}
            </div>
            <div class="col-5 text-truncate">
              {pendingRequest.request_description}
            </div>
          </div>
        {/each}
      {/if}
    </div>
  </div>
</div>
