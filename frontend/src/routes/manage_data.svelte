<script lang="ts">
  import { onMount } from "svelte";
  import dayjs from "dayjs";
  import RemoveShare, {Share, getShares} from "../api/shares";
  import Tasks from "../api/tasks";
  import Permissions from "../api/permissions";
    import File from "../components/File.svelte";
  import State from "../components/State.svelte";

  import Spinner from "../components/Spinner.svelte";

  let own_datasets: any[] | null = null;
  let data = {};
  let dataset_tasks = {};

  let datasets = {};
  let algorithms = {};
  let givenPermissions: { [index: string]: any } = {};

  onMount(async () => {
    updateUserFiles();
    
    dataset_tasks = await Tasks.getLogs().then(r => r.data.data_tasks);
    
    givenPermissions = await Permissions.getGivenPerFile().then(r => r.data.given_permissions);
  });

  async function updateUserFiles() {
    own_datasets = await getShares().then(r => r.own_datasets);
  }

  async function remove_permission(id: string, filename: string) {
    let { data: response } = await Permissions.remove(Number(id));

    let removed_permission = removeFromList(givenPermissions[filename], id);

    if (removed_permission.length === 0) {
      delete givenPermissions[filename];

      givenPermissions = givenPermissions;
    } else {
      givenPermissions[filename] = removed_permission;
    }
  }

  function removeFromList(fileList: any[], fileId: string) {
    for (let i = 0; i < fileList.length; i++) {
      if (fileList[i]["id"] === fileId) {
        fileList.splice(i, 1);
      }
    }
    return fileList;
  }

  function quickUpdate(fileId: string) {
    if (own_datasets !== null) {
      own_datasets = removeFromList(own_datasets, fileId);
    }
  }

  async function revokeFileShare(fileId: string) {
    quickUpdate(fileId);
    RemoveShare.remove(fileId).then(updateUserFiles);
  }
</script>

<svelte:head>
  <title>Manage Data</title>
</svelte:head>

<h3 class="display-5">Manage shared Files and Folders</h3>

<div class="container-fluid mx-auto m-2">

  {#if own_datasets === null}
    <Spinner />
  {:else}
    {#each own_datasets as file}
      <div class="row my-5 p-4">
        <div class="row">
          <div class="col-auto my-auto">
            <span class="fa-stack fa-2x text-primary">
              <i class="fas fa-circle fa-stack-2x" />
              <i class="fas fa-file fa-stack-1x fa-inverse" />
            </span>
            {file.name}
          </div>
          <div class="col">
            <button
              class="btn btn-danger rounded-xl font-weight-bold"
              on:click={() => revokeFileShare(file.id)}>
              <div class="px-4">Withdraw Data</div>
            </button>

          </div>

        </div>
        <div class="row mt-5 w-100">
          <div class="col-6 mx-5 p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Permissions</small>
            </h3>
            {#if givenPermissions !== null && givenPermissions[file.name] !== undefined}
              <div class="table-wrapper">
                <table class="table table-borderless">
                  <thead>
                    <th>With</th>
                    <th>Algorithm</th>
                    <th>Type</th>
                    <th />
                  </thead>
                  <tbody>

                    {#each givenPermissions[file.name] as permission}
                      <tr id={permission['id']} class="my-1">
                        <td>{permission.algorithm_provider}</td>
                        <td>{permission.algorithm}</td>
                        <td>{permission.permission_type}</td>
                        {#if permission.permission_type != "one time permission"}
                          <td class="text-danger font-weight-bold">
                            <a
                              class="text-danger"
                              href="#0"
                              on:click|preventDefault={() => remove_permission(permission.id, file.name)}>
                              Reject Permission
                            </a>
                          </td>
                        {/if}
                      </tr>
                    {/each}
                  </tbody>
                </table>
              </div>
            {:else}No permissions given on this file{/if}
          </div>
          <div class="col mx-5 p-3 rounded-xl background bg-lightgrey">
            <h3>
              <small class="text-muted">Runs</small>
            </h3>
            {#if dataset_tasks !== undefined && dataset_tasks[file.name] !== undefined}
              <div class="table-wrapper">
                <table class="table table-borderless">
                  <thead>
                    <th>Algorithm Owner</th>
                    <th>Passed</th>
                    <th>When</th>
                    <th>Action</th>
                  </thead>
                  <tbody>
                    {#each dataset_tasks[file.name] as task}
                    {#if task.state !== "stream_permission_request"}
                      <tr class="my-1">
                        <td>{task.author_email}</td>
                        <!-- {#if task.state === 'data_requested' || task.state === 'running'}
                          <td class="text-danger font-weight-bold">False</td>
                        {:else}
                          <td class="text-success font-weight-bold">True</td>
                        {/if} -->
                        <td><State state={task.state} /></td>

                        <td>
                          {dayjs(task.registered_on).format('DD-MM-YYYY HH:mm')}
                        </td>
                        <td class="text-primary font-weight-bold">
                          <a href={`/tasks/${task.id}`}>See log</a>
                        </td>
                      </tr>
                      {/if}
                    {/each}
                  </tbody>

                </table>
              </div>
            {:else}No runs done with this file{/if}
          </div>
        </div>
      </div>
    {:else}
      <div>You have shared no datasets</div>
    {/each}
  {/if}

</div>
