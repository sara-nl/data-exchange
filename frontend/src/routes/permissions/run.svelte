<script lang="ts">
  import Permissions from "../../api/permissions";
  import Tasks from "../../api/tasks";
  import { goto } from "@sapper/app";

  let obtained_permissions: [any] | null = null;

  let algorithms: [any] | null = [];
  let algorithm_files: [any] | null = null;
  let dataset_files = [];

  let permission = "";

  let data = {
    per_file: true
  };

  getUserPermissions();

  async function getUserPermissions() {
    try {
      let { data: response } = await Permissions.get_per_file();
      obtained_permissions = response.obtained_permissions;
      algorithms = Object.keys(obtained_permissions);
    } catch (error) {
      console.log(error.toString());
    }

    return false;
  }

  function run_with_perm() {
    let total_permission =
      obtained_permissions[data.algorithm_file][permission];
    let perm_id = total_permission.id;
    try {
      let { data: response } = Tasks.start_with_perm(perm_id, total_permission);
      goto("/tasks");
    } catch (error) {
      console.log(error.toString());
    }
    return false;
  }
</script>

<svelte:head>
  <title>DEX</title>
</svelte:head>

<h2 class="display-5">
  Run algorithm_files
  <small class="text-muted">with obtained permissions</small>
</h2>
<br />

<div class="container">
  <div class="row">
    <div class="col-md-4">
      <form>

        <div class="form-group">
          <label for="algorithm-file">
            <h3 class="display-6">Algorithm</h3>
            <select
              bind:value={data.algorithm_file}
              class="form-control"
              id="algorithm-file">

              {#if algorithms == null}
                <option value="">Fetching algorithms</option>
              {:else if algorithms.length > 0}
                <option value="">Select algorithm</option>

                {#each algorithms as file}
                  <option value={file}>{file}</option>
                {/each}
              {:else}
                <option value="">No algorithms available</option>
              {/if}

            </select>
          </label>
        </div>

        <div class="form-group">
          <label for="data-file">

            <h3 class="display-6">Permissions</h3>
            <select
              bind:value={permission}
              class="form-control"
              id="data-file"
              disabled={!data.algorithm_file}>

              {#if !data.algorithm_file}
                <option value="">Select algorithm first</option>
              {:else if obtained_permissions[data.algorithm_file].length > 0}
                <option value="">Select permission</option>

                {#each obtained_permissions[data.algorithm_file] as file, i}
                  <option value={i}>{file.dataset}/{file.algorithm}</option>
                {/each}
              {:else}
                <option value="">No permissions</option>
              {/if}
            </select>
          </label>
        </div>
      </form>

      <button
        class="form-control btn btn-primary"
        disabled={permission === ''}
        on:click={run_with_perm}>
        {'Run!'}
      </button>
    </div>
    <div class="col-xl-14 col-md-7 border p-3">
      <h4 class="text-muted">Permission info</h4>
      {#if permission !== ''}
        <div class="my-3">
          <h5>Permission given by</h5>
          {obtained_permissions[data.algorithm_file][permission].dataset_provider}
          <h5>Permission given to</h5>
          <b>You</b>
        </div>

        <div class="my-3">
          <h5>Dataset</h5>
          {obtained_permissions[data.algorithm_file][permission].dataset}
          <h5>Algorithm</h5>
          {obtained_permissions[data.algorithm_file][permission].algorithm}
        </div>
        <div class="my-3">
          <h5>Review output</h5>
          {obtained_permissions[data.algorithm_file][permission].review_output}
        </div>
      {/if}
    </div>
  </div>
</div>
