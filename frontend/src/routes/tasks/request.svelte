<script lang="ts">
    import { onMount } from "svelte";
    import { goto } from "@sapper/app";

    import LoadFiles from "../../api/loader";
    import Tasks, { TasksStartRequest } from "../../api/tasks";
    import Spinner from "../../components/Spinner.svelte";

    let state_color = {
        "request_rejected": "danger",
        "output_rejected": "warning",
        "output_released": "success",
        "running": "info"
    }

    let algorithm_files = null;
    let data = new TasksStartRequest();
    let requesting = false;

    onMount(async () => {
        await getUserFiles();
    });

    async function getUserFiles(){
        let { data } = await LoadFiles.start();
        algorithm_files = data.output.own_algorithms;
    }

    async function createRequest(event: any) {
        requesting = true;
        event.preventDefault();

        try {
            await Tasks.start(data);
            goto("/tasks");
        } catch (error) {
            requesting = false;
            throw error;
        }
    }
</script>

<svelte:head>
    <title>My Files</title>
</svelte:head>

<h2 class="display-5">
    Request use of a dataset
    <small class="text-muted">with one of your algorithms</small>
</h2>

<div class="container">
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
                                <option value="">Select algorithm</option>

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
