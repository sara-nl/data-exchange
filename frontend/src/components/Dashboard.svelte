<script lang="typescript">
  import Faq from './dashboard/Faq.svelte'
  import WhereToStart from './dashboard/WhereToStart.svelte'
  import FilesStats from './dashboard/FilesStats.svelte'
  import NextSteps from './dashboard/NextSteps.svelte'
  import { shares, mode } from '../stores'
  import Spinner from './Spinner.svelte'
</script>

<div class="container-fluid mx-auto">
  <div class="row">
    <div class="col-7">
      <h2 class="text-primary display-4">My Data Exchange</h2>
    </div>
    <div class="col-5 align-self-center">
      <FilesStats focus={$mode} algorithmsCnt={12} datasetsCnt={36} />
    </div>
  </div>
  <div class="row my-5">
    <div class="col px-5">
      {#if $shares === undefined}
        <Spinner />
      {:else if ($shares || []).length > 0}
        <NextSteps shares={$shares || []} />
      {:else}
        <WhereToStart />
      {/if}
    </div>
    <div class="col px-5">
      <Faq />
    </div>
  </div>
</div>
