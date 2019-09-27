<script lang="ts">
    import Runner from "../api/runner";
    import LoadRunner from "../api/loader";

    let running = false;

    let algorithm_files = []
    let dataset_files = []
    let errors = []

    let output = null

    let data = {
        algorithm_file: "",
        data_file: "",
    };

    getUserFiles()

    async function getUserFiles(){
        output=null
        try {
            let { data: response } = await LoadRunner.start(data);
            algorithm_files = response.output.algorithms;
            dataset_files = response.output.datasets;
            errors = response.output.errors;

            output = errors

        } catch (error) {
            output = error.response ? error.response.data : error.toString();
        }

        return false
    }

    async function handleClick() {
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

    function isValid() {
        return data.algorithm_file !== "" && data.data_file !== "";
    }
</script>

<svelte:head>
    <title>DEX</title>
</svelte:head>

<div class="container">
    <div class="row">
        <div class="col-xs-12 col-md-4">
            <form>

                <div class="form-group">
                    <label for="algorithm-file">
                        Algorithm file:
                        <select
                            class="form-control"
                            id="algorithm-file"
                            required>

                            {#each algorithm_files as file}
                                <option value={file}>{file}</option>
                            {/each}

                        </select>
                    </label>
                </div>

                <div class="form-group">
                    <label for="data-file">
                        Data file:
                        <select
                            class="form-control"
                            id="data-file"
                            bind:value={data.data_file}
                            required>

                            {#each dataset_files as file}
                                <option>{file}</option>
                            {/each}

                        </select>
                    </label>
                </div>

                <div class="form-group">
                    <a
                        href="#0"
                        class="form-control btn btn-primary"
                        disabled={running || !isValid()}
                        on:click={handleClick}
                    >
                        {running ? "Running..." : "Run!"}
                    </a>
                </div>
            </form>
        </div>

        <div class="col-xs-12 col-md-8">
            <pre>
                {output || "No output (yet)…"}
            </pre>
        </div>
    </div>
</div>
