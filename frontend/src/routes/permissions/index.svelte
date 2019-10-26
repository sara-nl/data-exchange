<script lang="ts">
    import { goto, stores } from "@sapper/app";

    import LoadFiles from "../../api/loader";
    import Tasks from "../../api/tasks";
    import Permissions from "../../api/permissions";
    import Spinner from "../../components/Spinner.svelte";

    let obtained_permissions: [any] | null = null
    let given_permissions: [any] | null = null

    getUserPermissions()

    async function getUserPermissions(){
        try {
            let { data: response } = await Permissions.get();
            obtained_permissions = response.obtained_permissions
            given_permissions = response.given_permissions

        } catch (error) {
            console.log(error.toString())
        }

        return false
    }

    async function remove_permission(id: number){

        try {
            let { data: response } = await Permissions.remove(id);
            obtained_permissions = response.obtained_permissions
            given_permissions = response.given_permissions

        } catch (error) {
            console.log(error.toString())
        }
    }

</script>



<svelte:head>
    <title>Permissions</title>
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css">
</svelte:head>


<h2 class="display-5">
    Overview of permissions
</h2>

{#if obtained_permissions === null || given_permissions === null}
<Spinner />
{:else}
<div class="container">
    <div class="row">
        <div class="col-md">
            <h2><small class="text-muted">Given Permissions</small></h2>
            <table class="table">
                <thead>
                    <tr>
                        <th scope="col">Given by</th>
                        <th scope="col">Given to</th>
                        <th scope="col">Dataset</th>
                        <th scope="col">Algorithm</th>
                        <th scope="col">Type</th>
                        <th scope="col"></th>
                    </tr>
                </thead>

                <tbody>
                    {#each given_permissions as file}
                    <tr>
                        <td><b>You</b></td>
                        <td>{file.algorithm_provider}</td>
                        <td>{file.dataset}</td>
                        <td>{file.algorithm}</td>
                        <td>
                          <strong>streaming:</strong> {file.stream ? "yes" : "no"}<br>
                          <strong>review output:</strong> {file.review_output ? "yes" : "no"}
                        </td>
                        <td>
                            <button class="close" on:click={() => remove_permission(file.id)}>
                                <span aria-hidden="true">&times;</span>
                            </button>
                        </td>
                    </tr>
                    {:else}
                    <tr>
                        <td colspan="6" class="text-center">You have given no permissions</td>
                    </tr>
                    {/each}
                </tbody>
                <br>
            </table>
        </div>
    </div>
    <div class="row">
        <div class="col">
            <h2><small class="text-muted">Obtained permissions</small></h2>
            <table class="table">
                <thead>
                    <tr>
                        <th scope="col">Permission given by</th>
                        <th scope="col">Permission given to</th>
                        <th scope="col">Dataset</th>
                        <th scope="col">Algorithm</th>
                    </tr>
                </thead>

                <tbody>
                    {#each obtained_permissions as file}
                    <tr>
                        <td>{file.dataset_provider}</td>
                        <td><b>You</b></td>
                        <td>{file.dataset}</td>
                        <td>{file.algorithm}</td>
                        <td></td>
                    </tr>
                    {:else}
                    <tr>
                        <td colspan="6" class="text-center">You have obtained no permissions</td>
                    </tr>
                    {/each}
                </tbody>
            </table>
        </div>
    </div>
</div>
{/if}
