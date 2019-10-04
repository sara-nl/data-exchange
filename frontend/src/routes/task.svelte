<script lang="ts">
    import LoadFiles from "../api/loader";
    import Tasks from "../api/tasks";

    let state_color = {
        "request_rejected": "danger",
        "output_rejected": "warning",
        "output_released": "success",
        "running": "info"
    }

    let task_id = Number(window.location.href.split(/[q=]+/)[1])

    var task;
    let owner = false

    let to_approve_requests = []
    let own_datasets = []

    let data = {
        data_file: "",
        updated_request: {},
        approved: false,
        released: false
    }

    let state = "Loading..."
    let requester = "Loading..."
    let data_owner = "Loading..."
    let algorithm = "Loading..."
    let dataset = "Loading..."
    let dataset_desc = "Loading..."
    let output;


    getTask()
    getUserFiles()

    async function getUserFiles(){
        try {
            let { data: response } = await LoadFiles.start();
            own_datasets = response.output.own_datasets;
        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function getTask(){
        try {
            let { data: response } = await Tasks.retrieve(task_id);
            task = response["task"]
            owner = response["owner"]

            state = task["state"]
            requester = task["author_email"]
            data_owner = task["approver_email"]
            algorithm = task["algorithm"]
            dataset = task["dataset"]
            dataset_desc = task["dataset_desc"]
            output = task["output"]


        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function review_request(approved: boolean) {
        data.approved = approved

        task["dataset"] = dataset
        data.updated_request = task

        try {
            let { data: response } = await Tasks.review(task_id, data);
            state = response["state"]
        } catch (error) {
            console.log(error.toString())
        }
    }

    async function release_output(released: boolean) {
        data.released = released

        try {
            let { data: response } = await Tasks.release(task["id"], data);
            state = response["state"]
        } catch (error) {
            console.log(error.toString())
        }

    }
</script>


<svelte:head>
    <title>My Files</title>
</svelte:head>

<h2 class="display-5">
    Request {task_id}
    <small class="text-{(state_color[state])}">{state}</small>
</h2>

<div class="container">
    <br>

    <div class="row">
        <div class="col">
            <div class="my-5">
                <h4>State</h4>
                {state}
            </div>
            <div class="my-5">
                <h4>Requester</h4>
                {requester}
                <h4>Data owner</h4>
                {data_owner}
            </div>
            <div class="my-5">
                <h4>Dataset description</h4>
                {dataset_desc}
            </div>
            <div class="my-5">
                <h4>Algorithm</h4>
                {algorithm}
                <h4>Dataset</h4>

                {#if owner && state === "data_requested"}
                    <select
                        class="form-control"
                        bind:value={dataset}
                        id="data-file"
                        >

                        {#if own_datasets}
                            <option value="">Select dataset</option>

                            {#each own_datasets as file}
                                <option value={file}>{file}</option>
                            {/each}
                        {:else}
                            <option value="">No datasets available</option>
                        {/if}
                    </select>
            {:else}
                {dataset}
            {/if}
            </div>
            {#if owner && state === "data_requested"}
            <button
                disabled = {!dataset}
                class="btn btn-success"
                on:click={() => review_request(true)}>
                Approve
            </button>
            <button
                class="btn btn-danger"
                on:click={() => review_request(false)}>
                Reject
            </button>
            {/if}

            {#if owner && state === "running"}
            <button
                class="btn btn-success"
                on:click={() => release_output(true)}>
                Release Output
            </button>
            <button
                class="btn btn-danger"
                on:click={() => release_output(false)}>
                Reject
            </button>
            {/if}
        </div>
        <div class="col-xs-12 col-md-6 border">
                <pre>
                    {output || "No output (yet)…"}
                </pre>
            </div>
    </div>

</div>
