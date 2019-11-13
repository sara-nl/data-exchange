<script lang="ts">
    import { onMount } from "svelte";
    import { goto } from "@sapper/app";

    import LoadFiles from "../../api/loader";
    import Permissions from "../../api/permissions"
    import Tasks, { TasksStartRequest } from "../../api/tasks";
    import Spinner from "../../components/Spinner.svelte";
    import PermissionInfo from "../../components/PermissionInfo.svelte";
    import ErrorMessage from "../../components/ErrorMessage.svelte";

    let state_color = {
        "request_rejected": "danger",
        "output_rejected": "warning",
        "output_released": "success",
        "running": "info"
    };

    let algorithm_files = null;
    let data = new TasksStartRequest();
    let permissions: any = null;
    let selected_permission: any = null;
    let requesting = false;
    let showError: any = null;

    // Vars for running algorithm with continuous permission.
    let continuous_data = {
        per_file: true,
        algorithm_file: "",
    };
    let obtainedPermissions: any = null;
    let algorithms: any = null;
    let selected_algorithm: any = null;
    let continuous_permission = "";
    let continuous_requesting = false;

    let running_tasks: any = null;

    onMount(async () => {
        await getPermissions();
        await getUserFiles();

        // Get continuous user permissions
        await getUserPermissions();
        await getPendingTasks();
    });

    async function getUserFiles(){
        LoadFiles.start().then(response => {
            algorithm_files = response.data.output.own_algorithms;
        });
    }

    async function getPermissions(){
        Permissions.list_permissions().then(response => {
            permissions = response.data.list_permissions
        });
    }

    async function createRequest(event: any) {
        requesting = true;
        event.preventDefault();

        try {
            Tasks.start(data).then( response => {
                getPendingTasks();
                clearForm();
            });

        } catch (error) {
            requesting = false;
            showError = error.response && error.response.data && error.response.data.error || null;
        }
    }

    function clearForm() {
        (<HTMLFormElement>document.getElementById("request-permission")).reset();
        (<HTMLInputElement>document.getElementById("permissions")).value="main";
        (<HTMLInputElement>document.getElementById("algorithm-file")).value="main";
    }


    async function getUserPermissions() {
        try {
          Permissions.get_obtained_per_file().then(permission_response => {
              obtainedPermissions = permission_response.data;
              algorithms = Object.keys(obtainedPermissions);
          });
        } catch (error) {
          console.log(error.toString());
        }
        return false;
    }

    async function getPendingTasks() {
        try {
            Tasks.get_pending_requests().then(task_response => {
                running_tasks = task_response.data;
                requesting = false;

            });
        } catch (error) {
            console.log(error.toString());
        }
        return false
    }


    async function runWithPermission(event: any) {
        event.preventDefault();
        if (obtainedPermissions === null) {
          return;
        }

        let totalPermission =
          obtainedPermissions[selected_algorithm].permissions[continuous_permission];
        totalPermission.algorithm = selected_algorithm;
        continuous_requesting = true;

        try {
          Tasks.start_with_perm(totalPermission.id, totalPermission).then(response => {
               goto("/manage_algorithms")
          });
        } catch (error) {
          console.log(error.toString());
        }
        continuous_requesting = false;
  }


</script>

<svelte:head>
    <title>My Files</title>
</svelte:head>

<ErrorMessage error={showError} />

<div class="row w-100 mb-2"><h3 class="display-5">Create request</h3></div>
<div class="row">
    <div class="col-6">
        <!-- Request permission -->
        <div class="row bg-primary text-white mr-4 rounded">
            <form id="request-permission" on:submit={createRequest}>
                <div class="row ml-1 font-weight-bold px-3 py-4">Request Permission for a dataset</div>

                <div class="row mb-3 ml-2 mr-3">
                    <div class="col-3 pl-2 w-100">Type of permission</div>
                    <div class="col-9">
                        <div class="container w-100">
                            {#if permissions === null}
                                <Spinner small />
                            {:else}
                                <select class="form-control bg-light text-dark custom-select rounded mr-sm-2"
                                        id="permissions"
                                        bind:value={data.permission}>
                                    <option selected="selected" disabled value="main">No permission selected</option>

                                    {#each permissions as permission}
                                        <option value={permission[0]}>{permission[1]}</option>
                                    {/each}
                                </select>
                            {/if}
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-lg-3 pl-2"></div>
                    <div class="col-9">
                        <div class="container my-0 py-0 mx-0 my-0">
                            <PermissionInfo permission={data.permission}/>
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
                                bind:value={data.algorithm}>
                                <option disabled selected="selected" value="main">Select algorithm</option>

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
                                bind:value={data.data_owner}>
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
                                    bind:value={data.dataset_desc}
                            ></textarea>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100">
                    <div class="col-12"><input
                            type="submit"
                            disabled={!(data.algorithm && data.data_owner && data.dataset_desc &&
                            data.permission) || requesting}
                            class="btn btn-success"
                            form="request-permission"
                            value={requesting ? "Requesting..." : "Request"} >
                    </div>
                </div>
            </form>
        </div>

        <!-- Running Requests -->
        <div class="row bg-light mr-4 rounded pb-5">
            <div class="row ml-1 px-3 py-4 font-weight-bold w-100">Pending Requests</div>
            <div class="row px-4 w-100 mb-2">
                <div class="col-4 font-weight-bold">Who</div>
                <div class="col-3 font-weight-bold">Type</div>
                <div class="col-5 font-weight-bold">Given Description</div>
            </div>
            {#if running_tasks === null || running_tasks.length === 0}
            <div class="row px-4 ml-1 w-100">
               Currently there are no pending requests.
            </div>
            {:else}
                {#each running_tasks as run_task}
                    <div class="row px-4 w-100 mb-1">
                        <div class="col-4">{run_task.approver_email}</div>
                        <div class="col-3">{run_task.permission.permission_type}</div>
                        <div class="col-5">{run_task.dataset_desc}</div>
                    </div>
                {/each}
            {/if}
        </div>
    </div>

    <!-- Continuous permission runner -->
    <div class="col-6 bg-light h-75 rounded">
        <div class="row font-weight-bold px-4 py-4">Run an algorithm with continuous permission</div>

        <form id="run-permission" on:submit={runWithPermission}>
            <div class="row ml-2 mr-3 w-100">
                <div class="col-lg-3 pl-2">Select algorithm</div>
                <div class="col-lg-9">
                    <div class="container">
                        {#if algorithms === null}
                            <Spinner small />
                        {:else if algorithms.length === 0}
                            No algorithms available.
                        {:else}
                            <select
                                class="form-control bg-primary text-white rounded select-white mr-sm-2"
                                id="algorithm-file"
                                bind:value={selected_algorithm}>
                                <option disabled selected="selected" value="">Select algorithm</option>

                                {#each algorithms as algorithm}
                                    <option value={algorithm}>{algorithm}</option>
                                {/each}
                            </select>
                        {/if}
                    </div>
                </div>
        </div>

        <div class="row my-3 ml-2 mr-3 w-100">
                <div class="col-lg-3 pl-2">Select dataset</div>
                <div class="col-lg-9">
                    <div class="container">
                        {#if !selected_algorithm}
                            Select algorithm first.
                        {:else if !obtainedPermissions[selected_algorithm].permissions}
                            No permissions.
                        {:else}
                            <select
                              bind:value={continuous_permission}
                              class="form-control bg-primary text-white rounded select-white mr-sm-2"
                              id="data-file"
                              disabled={!selected_algorithm}>
                              <option disabled value="">Select permission</option>

                              {#each obtainedPermissions[selected_algorithm].permissions as file, i}
                                <option value={i}>{file.dataset}/{selected_algorithm}</option>
                              {/each}
                            </select>
                          {/if}
                    </div>
                </div>
        </div>
        <div class="row my-3 ml-2 mr-3 w-100">
            <div class="col-12">
                <input
                    type="submit"
                    disabled={continuous_permission === '' || continuous_requesting}
                    class="btn btn-success"
                    form="run-permission"
                    value={continuous_requesting ? "Requesting..." : "Run"}>
                </div>
            </div>
        </form>
    </div>

</div>
