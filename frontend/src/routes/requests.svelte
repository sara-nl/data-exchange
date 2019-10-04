<script lang="ts">

    import LoadFiles from "../api/loader";
    import Tasks from "../api/tasks";
    import { goto, stores } from "@sapper/app";

    let state_color = {
        "request_rejected": "danger",
        "output_rejected": "warning",
        "output_released": "success",
        "running": "info"
    }

    let to_approve_requests = []
    let own_requests = []

    let data = {
        data_file: "",
        updated_request: {},
        approved: false
    }

    getUserTasks()

    async function getUserTasks(){
        try {
            let { data: response } = await Tasks.get();
            to_approve_requests = response.to_approve_requests
            own_requests = response.own_requests
        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    function see_details(id: number) {
        try {
            goto(`/task?id=${id}`)
        } catch (error) {
            console.log(error.toString())
        }

    }
</script>


<svelte:head>
    <title>My Files</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
</svelte:head>

        <h2 class="display-5">
            Overview
        </h2>
        <br>

<div class="container">
<div class="row">
        <div class="col-xl">
            <h2><small class="text-muted">Your requests</small></h2>
            <table class="table">
                <thead>
                    <tr>
                    <th scope="col">State</th>
                    <th scope="col">Dataset owner</th>
                    <th scope="col">Dataset description</th>
                    <th scope="col">Algorithm</th>
                    <th scope="col">Dataset</th>
                    <th scope="col"></th>
                    </tr>
                </thead>
                {#if own_requests.length > 0}
                    {#each own_requests as file}
                        <tbody>
                            <tr class="table-{(state_color[file.state])}">
                                    <td>{file.state}</td>
                                    <td>{file.approver_email}</td>
                                    <td>{file.dataset_desc}</td>
                                    <td>{file.algorithm}</td>
                                    <td>{file.dataset}</td>
                                    <td>
                                            <button
                                                class="btn btn-primary"
                                                on:click={() => see_details(file.id)}>
                                                See details
                                            </button>
                                    </td>
                            </tr>
                        </tbody>
                    {/each}
                {:else}
                    <div>You have no requests to review</div>
                {/if}
                <br>
            </table>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <h2><small class="text-muted">Requests to review</small></h2>
            <table class="table">
                <thead>
                    <tr>
                    <th scope="col">State</th>
                    <th scope="col">Requester</th>
                    <th scope="col">Dataset description</th>
                    <th scope="col">Algorithm</th>
                    <th scope="col">Dataset</th>
                    <th scope="col"></th>
                    </tr>
                </thead>

                {#if to_approve_requests.length > 0}
                    {#each to_approve_requests as file}
                        <tbody>
                            <tr>
                                    <td>{file.state}</td>
                                    <td>{file.author_email}</td>
                                    <td>{file.dataset_desc}</td>
                                    <td>{file.algorithm}</td>
                                    <td>{file.dataset}</td>
                                    <td>
                                            <button
                                                class="btn btn-primary"
                                                on:click={() => see_details(file.id)}>
                                                See details
                                            </button>
                                    </td>
                            </tr>
                        </tbody>
                    {/each}
                {:else}
                    <div>You have no requests to review</div>
                {/if}
                <br>
            </table>
        </div>
    </div>
</div>
