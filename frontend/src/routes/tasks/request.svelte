<script lang="ts">
    import { onMount } from "svelte";
    import { goto } from "@sapper/app";

    import LoadFiles from "../../api/loader";
    import Permissions from "../../api/permissions"
    import Tasks, { TasksStartRequest } from "../../api/tasks";
    import Spinner from "../../components/Spinner.svelte";
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

    let info_user_permission: string = "You request user based permission. If the data owner allows, " +
                                       "you can always run all your algorithms on the selected dataset.";
    let info_stream_permission: string = "You request continuous permission. If the data owner allows, " +
                                         "every change in the selected data (or data folder) will automatically" +
                                         " start a new run using this algorithm.";
    let info_run_once: string = "You request one time permission. The selected algorithm will be ran on the " +
                                "selected datset once.";

    onMount(async () => {
        await getPermissions();
        await getUserFiles();
    });

    async function getUserFiles(){
        let { data } = await LoadFiles.start();
        algorithm_files = data.output.own_algorithms;
    }

    async function getPermissions(){
        let { data } = await Permissions.list_permissions();
        permissions = data.list_permissions;
    }

    async function createRequest(event: any) {
        requesting = true;
        event.preventDefault();

        try {
            await Tasks.start(data);
            goto("/tasks");
        } catch (error) {
            requesting = false;
            showError = error.response && error.response.data && error.response.data.error || null;
        }
    }
</script>

<svelte:head>
    <title>My Files</title>
</svelte:head>

<ErrorMessage error={showError} />

<div class="row">
    <div class="col-6">
        <!-- Request permission -->
        <div class="row bg-primary text-white mr-4 rounded">
            <form on:submit={createRequest}>
                <div class="row px-4 py-4">Request Permission for a dataset</div>

                <div class="row mb-3 ml-2 mr-3 bg-dark w-100">
                    <div class="col-3 pl-2 bg-info">Type of permission</div>
                    <div class="col-5 bg-warning">
                        {#if permissions === null}
                            <Spinner small />
                        {:else}
                            <select class="form-control bg-light text-dark custom-select rounded mr-sm-2"
                                    id="permissions"
                                    bind:value={data.permission}>
                                <option selected="selected" disabled value="">Non chosen</option>

                                {#each permissions as permission}
                                    <option value={permission[0]}>{permission[1]}</option>
                                {/each}
                            </select>
                        {/if}
                    </div>
                    {#if selected_permission === "one time permission"}
                        <div class="col-4">{info_run_once}</div>
                    {:else if selected_permission === "stream permission"}
                        <div class="col-4">{info_stream_permission}</div>
                    {:else if selected_permission === "user permission"}
                        <div class="col-4">{info_user_permission}</div>
                    {:else}
                        <div class="col-4"></div>
                    {/if}
                </div>

                <div class="row my-3 ml-2 mr-3 w-100 bg-dark">
                    <div class="col-lg-3 pl-2 bg-info">Select algorithm</div>
                    <div class="col-lg-9 bg-warning">
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
                                <option disabled value="">Select algorithm</option>

                                {#each algorithm_files as file}
                                    <option value={file.name}>{file.name}</option>
                                {/each}
                            </select>
                        {/if}
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100 bg-dark">
                    <div class="col-3 pl-2 bg-info">Data owner email</div>
                    <div class="col-9 bg-warning">
                        <div class="container">
                            <input class="form-control"
                                type="text"
                                id="data_owner"
                                bind:value={data.data_owner}>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100 bg-dark">
                    <div class="col-3 pl-2 bg-info">Dataset description</div>
                    <div class="col-9 bg-warning">
                        <div class="container">
                            <textarea rows=5
                                    class="form-control"
                                    id="dataset_desc"
                                    bind:value={data.dataset_desc}>
                            </textarea>
                        </div>
                    </div>
                </div>

                <div class="row my-3 ml-2 mr-3 w-100 bg-dark">
                    <div class="col-12 bg-white"><input
                            type="submit"
                            disabled={!(data.algorithm && data.data_owner && data.dataset_desc &&
                            data.permission) || requesting}
                            class="btn btn-success"
                            value={requesting ? "Requesting..." : "Request data"} >
                    </div>
                </div>
            </form>
        </div>

        <!-- Running Requests -->
        <div class="row bg-light mr-4 rounded">
            <div class="row px-4 py-4">Running Requests</div>
        </div>
    </div>

    <!-- Continuous permission runner -->
    <div class="col-6 bg-light rounded">
        <div class="row px-4 py-4">Run a algorithm with continuous permission</div>
    </div>


</div>





<h4>OLD BELOW </h4>

<h2 class="display-5">
    Request use of a dataset
    <small class="text-muted">with one of your algorithms</small>
</h2>

<div class="container-fluid">
    <br>

    <div class="row">
        <div class="col">
            <form on:submit={createRequest}>
                <div class="form-group">
                    <label for="algorithm">
                        Algorithm
                        {#if algorithm_files === null}
                            <Spinner small />
                        {:else if algorithm_files.length === 0}
                            No algorithms available.
                        {:else}
                            <select
                                class="form-control"
                                id="algorithm-file"
                                bind:value={data.algorithm}
                            >
                                <option disabled value="">Select algorithm</option>

                                {#each algorithm_files as file}
                                    <option value={file.name}>{file.name}</option>
                                {/each}
                            </select>
                        {/if}
                    </label>
                </div>
                <div class="form-group">
                    <label for="data_owner">
                        Data owner
                        <input
                            class="form-control"
                            type="text"
                            id="data_owner"
                            bind:value={data.data_owner}
                        >
                    </label>
                </div>

                <div class="form-group">
                    <label for="dataset">
                        Description of dataset
                        <textarea rows=5
                            bind:value={data.dataset_desc}
                            class="form-control"
                            id="dataset_desc"
                        ></textarea>
                    </label>
                </div>

                <div class="form-group">
                    <input
                        type="submit"
                        disabled={!(data.algorithm && data.data_owner && data.dataset_desc) || requesting}
                        class="form-control btn btn-primary"
                        value={requesting ? "Requesting..." : "Request data"}
                    >
                </div>
            </form>
        </div>
        <br>
        <div class="col border">
            <h4 class="dispay-1">How a dataset request works:</h4>

            <p><b>1.</b> You select which algorithm you want to run, provide username of dataset owner and describe what dataset you want to use</p>
            <p><b>2.</b> The dataset owner will review your request and either approve or deny</p>
            <p><b>3.</b> If approved the algorithm will run and the output shown to the dataset owner</p>
            <p><b>4.</b> When the dataset owner has approved the output, it will be released to you</p>

            <p>You can follow the status of your request on the datarequest page.</p>
        </div>
    </div>

</div>
