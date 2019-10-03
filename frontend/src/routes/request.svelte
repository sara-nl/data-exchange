<script lang="ts">
    import LoadFiles from "../api/loader";
    import Tasks from "../api/tasks";

    let algorithm_files = []

    let data = {
        algorithm: "",
        data_owner: "",
        requested_data: ""
    }


    getUserFiles()

    async function getUserFiles(){
        try {
            let { data: response } = await LoadFiles.start();
            algorithm_files = response.output.own_algorithms;

        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function yeet(event:any){
        event.preventDefault();

        try {
            let { data: response } = await Tasks.start(data);
            console.log(response.output);
        } catch (error) {
            console.error(error)
        }

        console.log(data.algorithm)
        console.log(data.data_owner)
        console.log(data.requested_data)
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
        <div class="col-xs-12 col-md-4">
            <form on:submit={yeet}>
                <div class="form-group">
                    <label for="algorithm">
                        Algorithm
                        <select
                            class="form-control"
                            id="algorithm-file"
                            bind:value={data.algorithm}
                            >

                            {#if algorithm_files.length > 0}
                                <option value="">Select algorithm</option>

                                {#each algorithm_files as file}
                                    <option value={file}>{file}</option>
                                {/each}
                            {:else}
                                <option value="">No algorithms available</option>
                            {/if}

                        </select>
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
                        <textarea
                            bind:value={data.requested_data}
                            class="form-control"
                            id="requested_data"
                        ></textarea>
                    </label>
                </div>

                <div class="form-group">
                    <input
                        type="submit"
                        class="form-control btn btn-primary"
                        value={"Request data"}
                    >
                </div>
            </form>
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
