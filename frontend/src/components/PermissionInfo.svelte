<script lang="ts">
    let info_user_permission: string = "You can always run all the algorithms you've shared to DataExchange on the " +
                                       "selected dataset.";
    let info_stream_permission: string = "Every change in the selected data (or data folder) will automatically" +
                                         " start a new run using this algorithm.";
    let info_run_once: string = "The selected algorithm will be ran on the " +
                                "selected dataset of the data owner exactly once.";
    let info_standard: string = "You have to select a specific permission in order to make a request to a data" +
                                "owner.";

    let info_user_permission_data_owner: string = "The requesting user can run" +
                                                  " all his algorithms on the dataset you're going to select at " +
                                                  "any time.";
    let info_stream_permission_data_owner: string = "Every change in the selected data " +
                                                    "(or data folder) will automatically start a new run using the " +
                                                    "given algorithm.";
    let info_run_once_data_owner: string = "The selected algorithm will be ran on your " +
                                           "selected dataset once.";

    export let permission: string;
    export let user: string = "algorithm";
</script>

<div class="PermissionInfo px-0 my-0 mx-0 py-0">
    <div class="container px-0 my-0 mx-0 py-0">
        {#if permission === "one time permission"}
            {#if user === "algorithm"}
                {info_run_once}
            {:else}
                {info_run_once_data_owner}
            {/if}
        {:else if permission === "stream permission"}
            {#if user === "algorithm"}
                {info_stream_permission}
            {:else}
                {info_stream_permission_data_owner}
            {/if}
        {:else if permission === "One specific user permission"}
            {#if user === "algorithm"}
                {info_user_permission}
            {:else}
                {info_user_permission_data_owner}
            {/if}
        {:else}
            {#if user === "algorithm"}
                {info_standard}
            {/if}
        {/if}
    </div>
</div>