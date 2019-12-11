<script lang="ts">
    declare var $mode: string;

    import { goto, stores } from "@sapper/app";
    import { onMount } from "svelte";

    import Tasks from "../../api/tasks";
    import Spinner from "../../components/Spinner.svelte";
    import { token, mode } from "../../stores";

    let state_color = {
        "request_rejected": "danger",
        "release_rejected": "warning",
        "output_released": "success",
        "running": "info",
        "success": "info",
        "error": "danger",
    };

    let to_approve_requests: [any] | null = null
    let own_requests: [any] | null = null

    // This fixes $mode not working.
    // I haven't got the faintest of clues.
    mode;

    let data = {
        data_file: "",
        updated_request: {},
        approved: false
    }

    onMount(async () => {
        await getUserTasks();
    });

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
        goto(`/tasks/${id}`)
    }
</script>

<style>
    .table-hover tbody tr {
        cursor: pointer;
    }
</style>


<svelte:head>
    <title>My Files</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
</svelte:head>


<h2 class="display-5">
    Overview
</h2>

{#if to_approve_requests === null || own_requests === null}
<Spinner />
{:else}
<div class="container-fluid mx-auto">
    {#if $mode === "data"}
        <div class="row">
            <div class="col-xl">
                <h2><small class="text-muted">Requests to review</small></h2>
                <table class="table" class:table-hover={to_approve_requests.length > 0}>
                    <thead>
                        <tr>
                            <th scope="col">State</th>
                            <th scope="col">Requester</th>
                            <th scope="col">Dataset Description</th>
                            <th scope="col">Algorithm</th>
                            <th scope="col">Dataset</th>
                        </tr>
                    </thead>

                    <tbody>
                        {#each to_approve_requests as file}
                        <tr on:click={() => goto(`/tasks/${file.id}`)}>
                            <td>{file.state}</td>
                            <td>{file.author_email}</td>
                            <td>{file.dataset_desc}</td>
                            <td>{file.algorithm}</td>
                            <td>{file.dataset}</td>
                        </tr>
                        {:else}
                        <tr>
                            <td colspan="6" class="text-center">You have no requests to review</td>
                        </tr>
                        {/each}
                    </tbody>
                </table>
            </div>
        </div>
    {:else}
        <div class="row">
            <div class="col-xl">
                <h2><small class="text-muted">Your requests</small></h2>
                <table class="table" class:table-hover={own_requests.length > 0}>
                    <thead>
                        <tr>
                            <th scope="col">State</th>
                            <th scope="col">Dataset Owner</th>
                            <th scope="col">Dataset Description</th>
                            <th scope="col">Algorithm</th>
                            <th scope="col">Dataset</th>
                        </tr>
                    </thead>

                    <tbody>
                        {#each own_requests as file}
                        <tr
                            class={`table-${state_color[file.state]}`}
                            on:click={() => goto(`/tasks/${file.id}`)}
                        >
                            <td>{file.state}</td>
                            <td>{file.approver_email}</td>
                            <td>{file.dataset_desc}</td>
                            <td>{file.algorithm}</td>
                            <td>{file.dataset}</td>
                        </tr>
                        {:else}
                        <tr>
                            <td colspan="6" class="text-center">You have no active requests</td>
                        </tr>
                        {/each}
                    </tbody>
                    <br>
                </table>
            </div>
        </div>
    {/if}
</div>
{/if}
