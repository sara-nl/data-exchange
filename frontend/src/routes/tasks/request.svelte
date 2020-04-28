<script lang="ts">
    import { onMount } from "svelte";
    import { goto } from "@sapper/app";

    import { getShares } from "../../api/shares";
    import Tasks from "../../api/tasks"
    import { permissionTypeDict, permissionTypeLabels, requestPermission, getAllPermissions, PermissionRequest, Permission } from "../../api/permissions";
    import Spinner from "../../components/Spinner.svelte";
    import RunWithUserPermission from "../../components/RunWithUserPermission.svelte";
    import PermissionInfo from "../../components/PermissionInfo.svelte";
    import ErrorMessage from "../../components/ErrorMessage.svelte";

    let algorithm_files = null;
    let requesting = false;
    let showError: any = null;

    const requesterPermissionTypeDict = permissionTypeDict['algorithm']

    let newDataRequest: Partial<PermissionRequest> = {};
    let pendingRequests: Permission[] | null = null;

    onMount(async () => {
        getShares().then(response => {
            algorithm_files = response.own_algorithms;
        });
        
        getAllPermissions().then(({obtained_permissions}) => {
            pendingRequests = obtained_permissions.filter(r => 
            r.state === 'pending' || r.state === 'analyzing')
        })
    });

    async function createRequest(event: any) {
        requesting = true;
        requestPermission(newDataRequest as PermissionRequest).then(storedRequest => {
            pendingRequests = (pendingRequests === null) ? [storedRequest] : [storedRequest, ...pendingRequests]
            clearForm();
        }).catch(error => {
            requesting = false;
            clearForm();
            showError = error.response && error.response.data && error.response.data.error || null;
        });
    }

    function clearForm() {
        (<HTMLFormElement>document.getElementById("request-permission")).reset();
        (<HTMLInputElement>document.getElementById("permissions")).value="";
        (<HTMLInputElement>document.getElementById("algorithm-file")).value="";
    }

</script>

<svelte:head>
    <title>Create request</title>
</svelte:head>

<ErrorMessage error={showError} />

<div class="row w-100 mb-2"><h3 class="display-5">Create request</h3></div>
<div class="row">
    <div class="col-6">
        <!-- Request permission -->
        <div class="row bg-primary text-white mr-4 rounded">
            <form id="request-permission" on:submit|preventDefault={createRequest}>
                <div class="row ml-1 font-weight-bold px-3 py-4">Request Permission for a dataset</div>

                <div class="row mb-3 ml-2 mr-3">
                    <div class="col-3 pl-2 w-100">Type of permission</div>
                    <div class="col-9">
                        <div class="container w-100">
                            <select class="form-control bg-light text-dark custom-select rounded mr-sm-2"
                                    id="permissions"
                                    bind:value={newDataRequest.permission_type}>
                                <option selected="selected" disabled value="">No permission selected</option>

                                {#each Object.keys(permissionTypeLabels) as permissionType}
                                    <option value={permissionType}>{permissionTypeLabels[permissionType]}</option>
                                {/each}
                            </select>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-lg-3 pl-2"></div>
                    <div class="col-9">
                        <div class="container my-0 py-0 mx-0 my-0">
                        {#if newDataRequest.permission_type}
                            <PermissionInfo permission={newDataRequest.permission_type}/>
                        {:else}
                            You have to select a specific permission in order to make a request to a dataowner.
                        {/if}
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-lg-3 pl-2">Select algorithm</div>
                    <div class="col-lg-9">
                        <div class="container">
                            {#if algorithm_files === null}
                            <Spinner small />
                        {:else if algorithm_files.length === 0}
                            No algorithms available.
                        {:else}
                            <select
                                class="form-control bg-light text-black custom-select rounded mr-sm-2"
                                id="algorithm-file"
                                bind:value={newDataRequest.algorithm}>
                                <option disabled selected="selected" value="">Select algorithm</option>

                                {#each algorithm_files as file}
                                    <option value={file.name}>{file.name}</option>
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
                            <input class="form-control"
                                type="text"
                                id="data_owner"
                                bind:value={newDataRequest.dataset_provider}>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-3 pl-2">Dataset description</div>
                    <div class="col-9">
                        <div class="container">
                            <textarea rows=5
                                    class="form-control"
                                    id="dataset_desc"
                                    bind:value={newDataRequest.request_description}
                            ></textarea>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-12"><input
                            type="submit"
                            disabled={!(newDataRequest.algorithm && newDataRequest.dataset_provider && newDataRequest.request_description &&
                            newDataRequest.permission_type) || requesting}
                            class="btn btn-success"
                            form="request-permission"
                            value={requesting ? "Requesting..." : "Request"} >
                    </div>
                </div>
            </form>
        </div>

    </div>

    <RunWithUserPermission algorithms={algorithm_files} />
</div>
<div class="row">
        <!-- Running Requests -->
        <div class="row bg-light mr-4 rounded pb-5">
            <div class="row ml-1 px-3 py-4 font-weight-bold w-100">Pending Requests</div>
            <div class="row px-4 w-100 mb-2">
                <div class="col-4 font-weight-bold">Reviewer</div>
                <div class="col-3 font-weight-bold">Type</div>
                <div class="col-5 font-weight-bold">Given Description</div>
            </div>
            {#if pendingRequests === null || pendingRequests.length === 0}
            <div class="row px-4 ml-1 w-100">
               Currently there are no pending requests.
            </div>
            {:else}
                {#each pendingRequests as pendingRequest}
                    <div class="row px-4 w-100 mb-1">
                        <div class="col-4">{pendingRequest.dataset_provider}<br /> <a href="/requests/{pendingRequest.id}">View</a></div>
                        <div class="col-3">{permissionTypeLabels[pendingRequest.permission_type]}</div>
                        <div class="col-5">{pendingRequest.request_description}</div>
                    </div>
                {/each}
            {/if}
        </div>
</div>
