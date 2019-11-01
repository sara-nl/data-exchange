<script lang="ts">
  import { goto, stores } from "@sapper/app";
  import dayjs from "dayjs";

  import LoadFiles from "../api/loader";
  import Tasks from "../api/tasks";
  import Spinner from "../components/Spinner.svelte";

  let state_color = {
    request_rejected: "danger",
    release_rejected: "warning",
    output_released: "success",
    running: "info",
    success: "info",
    error: "danger"
  };

  let not_reviewed_yet: [any] | null = null;
  let reviewed: [any] | null = null;

  let data = {
    data_file: "",
    updated_request: {},
    approved: false
  };

  getUserTasks();

  async function getUserTasks() {
    try {
      let { data: response } = await Tasks.get_data_requests();
      not_reviewed_yet = response.not_reviewed_yet;
      reviewed = response.reviewed;
      console.log(response);
    } catch (error) {
      console.log(error.toString());
    }

    return false;
  }

  function see_details(id: number) {
    goto(`/tasks/${id}`);
  }
</script>

<style>
  .table-hover tbody tr {
    cursor: pointer;
  }
</style>

<svelte:head>
  <title>Requests</title>
  <link
    rel="stylesheet"
    href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" />
</svelte:head>

<h3 class="display-5">Review incoming requests</h3>

{#if not_reviewed_yet === null || reviewed === null}
  <Spinner />
{:else}
  <div class="container-fluid mt-2">
    <!-- Not Reviewed Yet Table -->
    <div class="row">
      <div class="col-xl mt-4">
        <h3>
          <small class="text-muted">Not Reviewed Yet</small>
        </h3>
        <div class="table-wrapper-xl">

          <table class="table table-borderless">
            <thead>
              <tr>
                <th class="normal_column" scope="col">Who</th>
                <th class="normal_column" scope="col">Type</th>
                <th scope="col" class="desc_column">Dataset description</th>
                <th class="normal_column" scope="col">Algorithm Name</th>
                <th class="normal_column" scope="col">When</th>
                <th class="normal_column" scope="col" />
                <th class="normal_column" scope="col">Action</th>
              </tr>
            </thead>

            <tbody>
              {#each not_reviewed_yet as file}
                <tr>
                  <td />
                </tr>
                <tr class="rounded-xl">

                  <td class="bg-lightgrey normal_column rounded-xll">
                    {file.approver_email}
                  </td>

                  {#if file.permission !== null}
                    <td class="bg-lightgrey normal_column">
                      {file.permission['permission_type']}
                    </td>
                  {:else}
                    <td class="bg-lightgrey normal_column">One Time Run</td>
                  {/if}

                  <td class="bg-lightgrey desc_column">{file.dataset_desc}</td>
                  <td class="bg-lightgrey normal_column">{file.algorithm}</td>
                  <td class="bg-lightgrey normal_column">
                    {dayjs(file.registered_on).format('DD-MM-YYYY')}
                  </td>
                  <td class="bg-lightgrey rounded-xlr normal_column" />
                  <td class="bg-lightgrey normal_column">
                    <button
                      class="btn btn-primary rounded-xl font-weight-bold"
                      on:click={() => goto(`/tasks/${file.id}`)}>

                      <div class="px-4">Review</div>
                    </button>
                  </td>

                </tr>
              {:else}
                <tr>
                  <td colspan="6" class="text-center">
                    You have no requests to review
                  </td>
                </tr>
              {/each}
            </tbody>
            <br />
          </table>
        </div>
      </div>
    </div>

    <!-- Reviewed Table -->
    <div class="row">
      <div class="col">
        <h3>
          <small class="text-muted">Reviewed</small>
        </h3>
        <div class="table-wrapper-xl">
          <table class=" table table-borderless">
            <thead>
              <tr>
                <th class="normal_column" scope="col">Who</th>
                <th class="normal_column" scope="col">Type</th>

                <th scope="col">Dataset description</th>
                <th class="normal_column" scope="col">Algorithm Name</th>
                <th class="normal_column" scope="col">When</th>
                <th class="normal_column" scope="col">
                  Folder/Algorithm Run on
                </th>
                <th class="normal_column" scope="col">Action</th>
              </tr>
            </thead>
            <tbody>
              {#each reviewed as file}
                <tr>
                  <td />
                </tr>
                <tr>
                  <td class="bg-lightgrey normal_column rounded-xll">
                    {file.author_email}
                  </td>
                  {#if file.permission !== null}
                    <td class="bg-lightgrey normal_column">
                      {file.permission['permission_type']}
                    </td>
                  {:else}
                    <td class="bg-lightgrey normal_column">One Time Run</td>
                  {/if}
                  <td class="bg-lightgrey desc_column">{file.dataset_desc}</td>
                  <td class="bg-lightgrey normal_column">{file.algorithm}</td>
                  <td class="bg-lightgrey normal_column">
                    {dayjs(file.registered_on).format('DD-MM-YYYY')}
                  </td>
                  <td class="bg-lightgrey normal_column rounded-xlr">
                    <span class="fa-stack fa-2x text-primary">
                      <i class="fas fa-circle fa-stack-2x" />
                      <i class="fas fa-file fa-stack-1x fa-inverse" />
                    </span>
                    {file.dataset}
                  </td>
                  <td class="bg-lightgrey rounded-xlr normal_column">
                    <button
                      class="btn btn-primary rounded-xl font-weight-bold"
                      on:click={() => goto(`/tasks/${file.id}`)}>

                      <div class="px-4">Details</div>
                    </button>
                  </td>
                </tr>
              {:else}
                <tr>
                  <td colspan="6" class="text-center">
                    You have reviewed requests
                  </td>
                </tr>
              {/each}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
{/if}
