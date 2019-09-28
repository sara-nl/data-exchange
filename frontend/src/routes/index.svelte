<script lang="ts">
    import Runner from "../api/runner";
    import LoadFiles from "../api/loader";

    let running = false;

    let algorithm_files = []
    let dataset_files = []
    let errors = []

    let output;

    let data = {
        algorithm_file: "",
        data_file: "",
    };

    getUserFiles()

    async function getUserFiles(){
        try {
            let { data: response } = await LoadFiles.start();
            algorithm_files = response.output.own_algorithms;
            dataset_files = response.output.available_datasets;

        } catch (error) {
            output = error.response ? error.response.data : error.toString();
        }

        return false
    }

    async function handleClick() {
        // TODO REWRITE with bindings
        data.algorithm_file = document.getElementById("algorithm-file").value
        data.data_file = document.getElementById("data-file").value

        if(data.algorithm_file == "" || data.data_file == "") {
            output = "You need an algorithm and dataset to run"
            return false
        }

        output = null;
        running = true;

        try {
            let { data: response } = await Runner.start(data);
            console.log(response.output);
            output = response.output;
        } catch (error) {
            output = error.response ? error.response.data : error.toString();
        }

        running = false;
        return false;
    }

</script>

<svelte:head>
    <title>DEX</title>
</svelte:head>

<h2 class="display-5">
    Run your algorithm
</h2>
<br>

<div class="container">
    <div class="row">
        <div class="col-xs-12 col-md-4">
            <form>

                <div class="form-group">
                    <label for="algorithm-file">
                        Algorithm:
                        <select
                            class="form-control"
                            id="algorithm-file"
                            >
                            <option value="">Select algorithm</option>

                            {#each algorithm_files as file}
                                <option value={file}>{file}</option>
                            {/each}

                        </select>
                    </label>
                </div>

                <div class="form-group">
                    <label for="data-file">
                        Datasets:
                        <select
                            class="form-control"
                            id="data-file"
                            >
                            <option value="">Select dataset</option>

                            {#each dataset_files as file}
                                <option value={file}>{file}</option>
                            {/each}

                        </select>
                    </label>
                </div>

                <div class="form-group">
                    <a
                        href="#0"
                        class="form-control btn btn-primary"
                        disabled={running}
                        on:click={handleClick}
                    >
                        {running ? "Running..." : "Run!"}
                    </a>
                </div>
            </form>
        </div>

        <div class="col-xs-12 col-md-8 border">
            <pre>
                {output || "No output (yet)…"}
            </pre>
        </div>
    </div>
</div>
