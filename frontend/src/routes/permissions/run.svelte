<script lang="ts">
  import { onMount } from "svelte";
  import { goto } from "@sapper/app";

  import Permissions from "../../api/permissions";
  import Tasks from "../../api/tasks";
  import Spinner from "../../components/Spinner.svelte";

  let obtainedPermissions: any = null;
  let algorithms: any = null;

  let permission = "";
  let requesting = false;

  let data = {
    per_file: true,
    algorithm_file: "",
  };

  onMount(async () => {
    await getUserPermissions();
  })

  async function getUserPermissions() {
    try {
      let { data: response } = await Permissions.get_obtained_per_file();
      obtainedPermissions = response;
      algorithms = Object.keys(obtainedPermissions);
    } catch (error) {
      console.log(error.toString());
    }

    return false;
  }

  async function runWithPermission() {
    if (obtainedPermissions === null) {
      return;
    }

    let totalPermission =
      obtainedPermissions[data.algorithm_file].permissions[permission];
    totalPermission.algorithm = data.algorithm_file

    requesting = true;

    try {
      await Tasks.start_with_perm(totalPermission.id, totalPermission);
      goto("/tasks");
    } catch (error) {
      console.log(error.toString());
    }

    requesting = false;
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

{#if obtainedPermissions === null}
  <Spinner />
{:else}
  <div class="container-fluid">
    <div class="row">
      <div class="col-md-4">
        <form>

          <div class="form-group">
            <label for="algorithm-file">
              <h3 class="display-6">Algorithm</h3>
              {#if !algorithms}
                No algorithms available.
              {:else}
                <select
                  bind:value={data.algorithm_file}
                  class="form-control"
                  id="algorithm-file"
                >
                  <option disabled value="">Select algorithm</option>
                  {#each algorithms as file}
                    <option value={file}>{file}</option>
                  {/each}
                </select>
              {/if}
            </label>
          </div>

          <div class="form-group">
            <label for="data-file">

              <h3 class="display-6">Permissions</h3>
              {#if !data.algorithm_file}
                Select algorithm first.
              {:else if !obtainedPermissions[data.algorithm_file].permissions}
                No permissions.
              {:else}
                <select
                  bind:value={permission}
                  class="form-control"
                  id="data-file"
                  disabled={!data.algorithm_file}
                >
                  <option disabled value="">Select permission</option>

                  {#each obtainedPermissions[data.algorithm_file].permissions as file, i}
                    <option value={i}>{file.dataset}/{data.algorithm_file}</option>
                  {/each}
                </select>
              {/if}
            </label>
          </div>
        </form>

        <button
          class="form-control btn btn-primary"
          disabled={permission === '' || requesting}
          on:click={runWithPermission}
        >
          {requesting ? "Requesting..." : "Run"}
        </button>
      </div>
      <div class="col-xl-14 col-md-7 border p-3">
        <h4 class="text-muted">Permission info</h4>
        {#if !data.algorithm_file}
          No file selected.
        {:else if permission === ""}
          No permission selected.
        {:else}
          <div class="my-3">
            <h5>Permission given by</h5>
            {obtainedPermissions[data.algorithm_file].permissions[permission].dataset_provider}
            <h5>Permission given to</h5>
            <b>You</b>
          </div>

          <div class="my-3">
            <h5>Dataset</h5>
            {obtainedPermissions[data.algorithm_file].permissions[permission].dataset}
            <h5>Algorithm</h5>
            {data.algorithm_file}
          </div>
          <div class="my-3">
            <h5>Permission type</h5>
            {obtainedPermissions[data.algorithm_file].permissions[permission].permission_type}
            <h5>Review output</h5>
            {obtainedPermissions[data.algorithm_file].permissions[permission].review_output}
          </div>
        {/if}
      </div>
    </div>
  </div>
{/if}
