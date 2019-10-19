<script lang="ts">
    import { onMount } from "svelte";

    import Runner from "../api/runner";
    import LoadFiles from "../api/loader";

    let own_algorithms: any = null;
    let own_datasets: any = null;

    let output;
    let running = false

    let data = {
        algorithm_file: "",
        data_file: "",
    };


    onMount(async () => {
        await load();
    });

    async function load() {

        getUserFiles()

    }

    async function getUserFiles(){
        try {
            let { data: response } = await LoadFiles.start();
            own_algorithms = response.output.own_algorithms;
            own_datasets = response.output.own_datasets;
        } catch (error) {
            console.log(error.toString())
        }

        return false
    }


</script>

<svelte:head>
    <title>DEX</title>
</svelte:head>

<h2 class="display-4 ym-5">
    DataExchange
</h2>
<h2>
<small class="text-muted">a SURFsara x Bit concept</small>
</h2>
<br>

<div class="container">
    <div class="row">
        <div class="col ym-5">
            <h3>Where to start?</h3>
            {#if own_datasets !== null && own_algorithms !== null}
                {#if own_algorithms.length == 0 && own_datasets.length == 0}
                    <div class="my-3">
                    <p>You haven't shared any files with the DataExchange<br>Click here to learn how to share files:</p>
                    <a class="btn btn-primary" href="/myfiles">Share files</a>

                    </div>
                {/if}


                {#if own_datasets.length > 0}
                    <div class="my-3">
                    <p>You have shared datasets with the DataExchange<br>Click here to see requests for your data:</p>
                    <a class="btn btn-primary" href="/tasks">See requests</a>
                    </div>
                {/if}

                {#if own_algorithms.length > 0}
                    <div class="my-3">

                    <p>You have shared algorithms with the DataExchange<br>Click here to make a request:</p>
                    <a class="btn btn-primary" href="/tasks/request">Make a request</a>
                    </div>
                {/if}
            {:else}
                Loading...
            {/if}

            <br>
            <h3 class="ym-3">How does the DataExchange work?</h3>

            <p>The DataExchange offers a controlled and safe third-party enviroment where dataset and algorithm providers can safely cooperate.
            On DataExchange algorithm providers can make a request to use certain data without ever being physically in control of it.
            </p>

            <h5>Full data sovereignty</h5>
            <p>If dataset provider decides the accept the request both algorithm and dataset are loaded and placed in a secure container without any access to the outside.
            Here they can safely interact without the algorithm provider or anyone else every having hold of the dataset.
            The dataset provider can monitor this process and review the output before it is released thus remaining in control of his data. </p>

        </div>
    </div>
</div>
