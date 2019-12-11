<script lang="ts">
  import Spinner from "./Spinner.svelte";
  
// This component shows the progress of a single task

  export let currentStep: number;
</script>

<div class="col-sm-12 bg-primary text-white rounded">
  Run algorithm
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 1} text="Creating container" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 2} text="Installing dependencies" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 3} text="Downloading data and algorithm to container" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 4} text="Blocking all outside access to container" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 5} text="Verifying algorithm" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 6} text="Running algorithm on data" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 7} text="Saving output" />
</div>
<div class="Row ml-2">
  <Spinner small loading={currentStep <= 8} text="Deleting container including data and algorithm" />
</div>
<div class="Row ml-2">
<!-- Never stop showing this state. When everything is done, task progress 
component will be removed alltogether. -->
  <Spinner small loading={true} text="Wrapping up.." />
</div>