<script lang="ts">
  import { goto, stores } from '@sapper/app'

  import Tasks from '../../api/tasks'
  import Permissions from '../../api/permissions'
  import Spinner from '../../components/Spinner.svelte'

  let inbound: [any] | null = null
  let outbound: [any] | null = null

  let state_color = {
    rejected: 'danger',
    active: 'success',
  }

  getUserPermissions()

  async function getUserPermissions() {
    try {
      let { data: response } = await Permissions.get()
      inbound = response.inbound
      outbound = response.outbound
    } catch (error) {
      console.log(error.toString())
    }

    return false
  }

  async function remove_permission(id: number) {
    try {
      let { data: response } = await Permissions.remove(id)
      inbound = response.inbound
      outbound = response.outbound
    } catch (error) {
      console.log(error.toString())
    }
  }
</script>

<svelte:head>
  <title>Permissions</title>
</svelte:head>

<h2 class="display-5">Overview of permissions</h2>

{#if inbound === null || outbound === null}
  <Spinner />
{:else}
  <div class="container-fluid mx-auto">
    <div class="row">
      <div class="col-md">
        <h2><small class="text-muted">Given Permissions</small></h2>
        <table class="table">
          <thead>
            <tr>
              <th scope="col">Type</th>
              <th scope="col">Given By</th>
              <th scope="col">Given To</th>
              <th scope="col">Dataset</th>
              <th scope="col">Algorithm</th>
              <th scope="col">Permission State</th>
              <th scope="col" />
            </tr>
          </thead>

          <tbody>
            {#each outbound as file}
              <tr>
                <td>{file.permission_type}</td>
                <td><b>You</b></td>
                <td>{file.algorithm_provider}</td>
                <td>{file.dataset}</td>
                <td>{file.algorithm}</td>
                <td class="text-{state_color[file.state]}">{file.state}</td>
                <td>
                  <button
                    class="close"
                    on:click={() => remove_permission(file.id)}>
                    <span aria-hidden="true">&times;</span>
                  </button>
                </td>
              </tr>
            {:else}
              <tr>
                <td colspan="6" class="text-center">
                  You have given no permissions
                </td>
              </tr>
            {/each}
          </tbody>
          <br />
        </table>
      </div>
    </div>
    <div class="row">
      <div class="col">
        <h2><small class="text-muted">Obtained permissions</small></h2>
        <table class="table">
          <thead>
            <tr>
              <th scope="col">Type</th>
              <th scope="col">Given by</th>
              <th scope="col">Given to</th>
              <th scope="col">Dataset</th>
              <th scope="col">Algorithm</th>
              <th scope="col">Permission state</th>
              <th scope="col" />
            </tr>
          </thead>

          <tbody>
            {#each inbound as file}
              <tr>
                <td>{file.permission_type}</td>
                <td>{file.dataset_provider}</td>
                <td><b>You</b></td>
                <td>{file.dataset}</td>
                <td>{file.algorithm}</td>
                <td class="text-{state_color[file.state]}">{file.state}</td>
              </tr>
            {:else}
              <tr>
                <td colspan="6" class="text-center">
                  You have obtained no permissions
                </td>
              </tr>
            {/each}
          </tbody>
        </table>
      </div>
    </div>
  </div>
{/if}
