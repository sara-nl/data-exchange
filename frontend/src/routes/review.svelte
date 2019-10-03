<script lang="ts">
    import LoadFiles from "../api/loader";
    import Tasks from "../api/tasks";

    let to_approve_requests = []
    let own_datasets = []
    let data = {
        data_file: "",
        updated_request: {},
        approved: false
    }

    let own_datasets_amount = 0


    getUserFiles()
    getUserTasks()

    async function getUserFiles(){
        try {
            let { data: response } = await LoadFiles.start();
            own_datasets = response.output.own_datasets;
        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function getUserTasks(){
        try {
            let { data: response } = await Tasks.get();
            to_approve_requests = response.to_approve_requests
        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function approve(id: number, approved: boolean) {
        data.updated_request = to_approve_requests[id]
        data.approved = approved

        try {
            let { data: response } = await Tasks.review(data.updated_request.id, data);
            to_approve_requests[id].state = response.output
        } catch (error) {
            console.log(error.toString())
        }

    }
</script>


<svelte:head>
    <title>My Files</title>
</svelte:head>

<h2 class="display-5">
    Requests
    <small class="text-muted">to review</small>
</h2>

<div class="container">
    <br>

    <div class="row">
        <div class="col">
            <table class="table">
                <thead>
                    <tr>
                    <th scope="col">State</th>
                    <th scope="col">Requester</th>
                    <th scope="col">Requester algorithm</th>
                    <th scope="col">Requested description</th>
                    <th scope="col">Dataset</th>

                    </tr>
                </thead>
            {#if to_approve_requests.length > 0}
                {#each to_approve_requests as file, i}

                    <tbody>
                        <tr>
                                <td>{file.state}</td>
                                <td>{file.author_email}</td>
                                <td>{file.algorithm}</td>
                                <td>{file.dataset_desc}</td>
                                <td>
                                    <select
                                        bind:value={file.dataset}
                                        id="data-file"
                                        >

                                        {#if own_datasets.length > 0}
                                            <option value="">Select dataset</option>

                                            {#each own_datasets as file}
                                                <option value={file}>{file}</option>
                                            {/each}
                                        {:else}
                                            <option value="">No datasets available</option>
                                        {/if}
                                    </select>
                                </td>
                                <td>
                                        {#if file.state === "data_requested"}
                                        <button
                                            class="btn btn-success"
                                            on:click={() => approve(i, true)}>
                                            Approve
                                        </button>
                                        <button
                                            id="{i}"
                                            class="btn btn-danger"
                                            on:click={() => approve(i, false)}>
                                            Reject
                                        </button>
                                        {/if}
                                </td>
                        </tr>
                    </tbody>
                {/each}
            {:else}
                <div>You have no requests to review</div>
            {/if}
            <br>

        </div>
    </div>

</div>
