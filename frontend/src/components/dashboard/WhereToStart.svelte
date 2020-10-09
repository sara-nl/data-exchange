<script lang="typescript">
  import { token, mode, email } from '../../stores'
  import StartStep from './StartStep.svelte'
</script>

<div class="p-4 bg-lightgrey rounded-xl">
  <h3 class="mb-3">Where to start?</h3>
  {#if $mode == 'algorithm'}
    <StartStep title="☝️ Share your algorithms">
      <p>
        Upload your algorithms to SURF Research Drive or Google Drive and share
        them with the Data Exchange service account.
      </p>
      <div class="text-right">
        <a
          href="javascript:void(0)"
          on:click={() => mode.set('data')}
          class="card-link muted">I want to share datasets instead</a>
      </div>
    </StartStep>

    <StartStep title="Make a dataset request">
      <p>
        Once you shared at least one algorithm with Data Excahnge, get in touch
        with a data owner and request a one-time or continuous access to the
        data.
      </p>
    </StartStep>
    <StartStep title="Get notified when your request is reviewed">
      <p>
        Data Exchange will email you once the data owner have shared the output
        and (dependent on the permission you requested) you may get continuous
        access to a dataset or a collection of datasets.
      </p>
    </StartStep>
  {:else}
    <StartStep title="☝️ Share your datasets">
      <p>
        Upload your datasets to SURF Research Drive or Google Drive and share
        them with the Data Exchange service account. Your data won't be shared
        with anybody else at this point.
      </p>
      <div class="text-right">
        <a
          href="javascript:void(0)"
          on:click={() => mode.set('algorithm')}
          class="card-link muted">I want to share algorithms instead</a>
      </div>
    </StartStep>

    <StartStep title="Wait for incoming requests">
      <p>
        Data Exchange will send you an email when someone requests access to
        your datasets.
      </p>
    </StartStep>
  {/if}
</div>
