<script lang="ts">
    import Runner from "../api/runner";
    import LoadRunner from "../api/loader";

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
            let { data: response } = await LoadRunner.start();
            algorithm_files = response.output.algorithms;
            dataset_files = response.output.datasets;
            errors = response.output.errors;

            if(errors != []) {
                output = errors
            }

        } catch (error) {
            output = error.response ? error.response.data : error.toString();
        }

        return false
    }

    async function handleClick() {
        output = null;
        running = true;

        // TODO REWRITE with bindings
        let algorithm_select = document.getElementById("algorithm-file")
        data.algorithm_file = algorithm_select.options[algorithm_select.selectedIndex].value

        let data_select = document.getElementById("data-file")
        data.data_file = data_select.options[data_select.selectedIndex].value

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
                            >

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
                            >

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

        <div class="col-xs-12 col-md-8">
            <pre>
                {output || "No output (yet)…"}
            </pre>
        </div>
    </div>
</div>
