<script lang="ts">
    export let state: string;
    let class_;

    $: class_ {
        switch (state) {
        case "running":
        case "registered":
        case "data_requested":
        case "analyzing_algorithm":
        case "success":
        case "error":
            class_ = "";
            break;

        case "output_released":
            class_ = "text-success";
            break;

        case "request_rejected":
        case "release_rejected":
            class_ = "text-danger";
        }
    }

    $: state {
        switch (state) {
        case "running":
            state = "Running";
            break;

        case "registered":
        case "data_requested":
        case "analyzing_algorithm":
            state = "Requested";
            break;

        case "success":
        case "error":
            state = "Reviewing";
            break;

        case "output_released":
            state = "Passed";
            break;

        case "request_rejected":
        case "release_rejected":
            state = "Rejected";
        }
    }
</script>

<span class={class_}>{state}</span>
