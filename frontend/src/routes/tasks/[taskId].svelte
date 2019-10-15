<script lang="ts">
    import { onMount } from "svelte";
    import { stores } from "@sapper/app";
    import * as hljs from "highlight.js";

    import LoadFiles from "../../api/loader";
    import Tasks, { TasksReviewRequest } from "../../api/tasks";

    const { page } = stores();
    const { taskId } = $page.params;

    let state_color = {
        "request_rejected": "danger",
        "release_rejected": "warning",
        "output_released": "success",
        "running": "info",
        "success": "info",
        "error": "danger",
    };

    let visible: boolean = true;
    let ownDatasets: any = null;
    let task: any = null;

    let data = new TasksReviewRequest();

    onMount(async () => {
        await load();
        hljs.initHighlighting();
    });

    async function load() {
        const { data } = await Tasks.retrieve(taskId);
        task = data;

        if (task.state === "data_requested") {
            const { data } = await LoadFiles.start();
            ownDatasets = data.output.own_datasets;
        }
    }

    async function review_request(approved: boolean) {
        data.approved = approved;
        data.updated_request = task;

        try {
            if(approved) {
                task.output = "Running the algorithm"
                task.state = "running"
            }

            let { data: response } = await Tasks.review(taskId, data);
            task.state = response.state;

            if(response.output) {
                task.output = response.output
            }
        } catch (error) {
            console.log(error.toString());
        }

        // Reload task after reviewing.
        // await load();
    }

    async function release_output(released: boolean) {
        data.released = released;

        try {
            let { data: response } = await Tasks.release(taskId, data);
            task.state = response.state;
        } catch (error) {
            console.log(error.toString())
        }

        // Reload task after reviewing.
        await load();
    }
</script>


<svelte:head>
    <link rel="stylesheet" href="atom-one-light.css">
    <title>My Files</title>
</svelte:head>


{#if task === null}
<h3>Loading...</h3>
{:else}
<h2 class="display-5">
    Request {taskId}
    <small class="text-{state_color[task.state]}">{task.state}</small>
</h2>

<div class="container">
    <div class="row">
        <div class="col-4">
            <div class="my-5">
                <h4>State</h4>
                {task.state}
            </div>
            <div class="my-5">
                <h4>Requester</h4>
                {task.author_email}
                <h4>Data owner</h4>
                {task.approver_email}
            </div>
            <div class="my-5">
                <h4>Dataset description</h4>
                {task.dataset_desc}
            </div>
            <div class="my-5">
                <h4>Algorithm</h4>
                <ul style="list-style:none; padding-left: 0;">
                    <li>{task.algorithm}</li>
                    <li><button class="btn btn-primary" on:click={() => visible =!visible}>
                    {#if visible} Show output
                    {:else} Show algorithm
                    {/if}
                    </button></li>
                </ul>

                <h4>Dataset</h4>
                {#if task.is_owner && task.state === "data_requested"}
                    {#if ownDatasets === null}
                        Loading…
                    {:else if ownDatasets.length === 0}
                        No datasets available.
                    {:else}
                        <select
                            class="form-control"
                            bind:value={task.dataset}
                            id="data-file"
                        >
                            <option value="">Select dataset</option>

                            {#each ownDatasets as file}
                                <option value={file}>{file}</option>
                            {/each}
                        </select>
                    {/if}
                {:else}
                    {task.dataset || "No dataset selected"}
                {/if}
            </div>

            {#if task.state === "data_requested"}
                {#if task.is_owner}
                    <button
                        disabled={!task.dataset}
                        class="btn btn-success"
                        on:click={() => review_request(true)}>
                        Approve
                    </button>
                    <button
                        class="btn btn-danger"
                        on:click={() => review_request(false)}>
                        Reject
                    </button>
                {:else}
                    <h4>Waiting for the data provider to review the algorithm…</h4>
                {/if}
            {/if}

            {#if task.state === "success" || task.state === "error"}
                {#if task.is_owner}
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
                {:else}
                    <h4>Waiting for the data provider to review the output…</h4>
                {/if}
            {/if}
        </div>
            <div hidden={!visible} class="col-12 col-md-8 border" style="padding-top: 20px;">
                <pre><code class="python">{task.algorithm_content || "No algorithm (yet)…"}</code></pre>
                <hr>
                <h5>{task.algorithm_info}</h5>
            </div>
            <div hidden={visible} class="col-12 col-md-8 border" style="padding-top: 20px;">
                <pre>{task.output || "No output (yet)…"}</pre>
            </div>
    </div>
</div>
{/if}
