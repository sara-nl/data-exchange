<script lang="ts">
    import LoadFiles from "../api/loader";

    let own_algorithms = []
    let own_datasets = []
    let data = {}

    let own_datasets_amount = 0


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
        <div class="col-xs-12 col-md-4">
            <form on:submit={submit}>
                <div class="form-group">
                    <label for="Algorithm">
                        Which will you be using?
                        <input
                            class="form-control"
                            type="text"
                            id="algorithm"
                            bind:value={data.username}
                        >
                    </label>
                </div>

                <div class="form-group">
                    <label for="password">
                        What data do you want request:
                        <input
                            class="form-control"
                            id="password"
                            type="textarea"
                            bind:value={data.password}
                        >
                    </label>
                </div>

                <div class="form-group">
                    <input
                        type="submit"
                        class="form-control btn btn-primary"
                        value={loading ? "Please wait..." : "Sign in"}
                        disabled={loading}
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
