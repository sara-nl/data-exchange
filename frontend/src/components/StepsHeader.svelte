
<script lang="ts">
	import { Permission } from "../api/permissions";
    export let activeStep: number;
</script>

<div class="row my-5 mx-auto border border-primary rounded">
	<div class="col-sm-3 text-center p-2 font-weight-bold {activeStep === 0 ? 'bg-primary text-white': ''}">
		Step 0. Analysis
	</div>
	<div class="col-sm-3 text-center p-2 font-weight-bold {activeStep === 1 ? 'bg-primary text-white': ''}">
		Step 1. Accept algorithm
	</div>
	<div class="col-sm-3 text-center p-2 font-weight-bold {activeStep === 2 ? 'bg-primary text-white': ''}">
	Step 2. Run algorithm
	</div>
	<div class="col-sm-3 text-center p-2 font-weight-bold {activeStep === 3 ? 'bg-primary text-white': ''}">
		Step 3. Release output
	</div>
</div>