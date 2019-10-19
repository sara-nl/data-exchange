<script lang="ts">
    import LoadFiles from "../api/loader";
    import Spinner from "../components/Spinner.svelte";

    let own_algorithms = null
    let own_datasets = null
    let data = {}


    getUserFiles()

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
    <title>My Files</title>
</svelte:head>

<h2 class="display-5">
    Your algorithms and datasets
    <small class="text-muted">shared with DataExchange</small>
</h2>

<div class="container">
    <br>

    <div class="row">
        <div class="col">
            {#if own_algorithms === null || own_datasets === null}
                <Spinner />
            {:else}
                <h3>Algorithms:</h3>
                {#each own_algorithms as file}
                    <div>{file}</div>
                {:else}
                    <div>You have shared no algorithms</div>
                {/each}
                <br>

                <h3>Datasets:</h3>
                {#each own_datasets as file}
                    <div>{file}</div>
                {:else}
                    <div>You have shared no datasets</div>
                {/each}
            {/if}
        </div>
        <br>
        <div class="col border">
            <h4 class="dispay-1">How to share files:</h4>

            <p><b>1.</b> Register and activate account with the <u>same email</u> as on ResearchDrive</p>
            <p><b>2.</b> In <a href="https://researchdrive.surfsara.nl">ResearchDrive</a> click on the share icon next to the file</p>
            <p><b>3.</b> Type in "Data Exchange" as users or groups to share with</p>
            <p><b>4.</b> Select "Data Exchange" to share your file</p>
            <p><b>5.</b> Refresh this page to see your file as being shared</p>
        </div>
    </div>

</div>
