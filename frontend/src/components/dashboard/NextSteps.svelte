<script lang="typescript">
  import { token, mode, email } from '../../stores'
  import type { Share } from '../../api/shares'
  import StartStep from './StartStep.svelte'

  export let shares: Share[]

  $: algorithms = shares.filter((s) => s.isAlgorithm)
  $: datasets = shares.filter((s) => !s.isAlgorithm)
</script>

<div class="p-4 bg-lightgrey rounded-xl">
  <h3 class="mb-3">Next steps</h3>
  {#if $mode === 'algorithm'}
    {#if algorithms.length == 0}
      <StartStep title="☝️ Share your algorithms">
        <p>
          You have shared
          {datasets.length}
          dataset(s), but your current user role supposes requesting access to
          data and you should share at least one algorithm before you can do
          that.
        </p>
        <div class="text-right">
          <a
            href="javascript:void(0)"
            on:click={() => mode.set('data')}
            class="card-link">I want to share datasets</a>
        </div>
      </StartStep>
    {/if}

    <StartStep title="Make a dataset request">
      <p>
        Get in touch with a data owner and request a one-time ticket or
        continuous access to the data.
      </p>

      {#if algorithms.length > 0}
        <div class="text-right">
          <a href="/tasks/request" class="card-link">Request access to data</a>
        </div>
      {/if}
    </StartStep>
    <StartStep title="Get notified when your request is reviewed">
      <p>
        Data Exchange will inform you about data owner's decision by email. You
        will eventually receive the output of your algorithm and dependent on
        the permission you requested, access to a dataset or a stream of data
        sets.
      </p>
    </StartStep>
  {:else}
    {#if datasets.length === 0}
      <StartStep title="☝️ Share your datasets">
        <p>
          You have shared
          {algorithms.length}
          algorithm(s), but your current user role supposes sharing datasets.
        </p>
        <div class="text-right">
          <a
            href="javascript:void(0)"
            on:click={() => mode.set('algorithm')}
            class="card-link muted">I want to request access to data</a>
        </div>
      </StartStep>
    {/if}

    <StartStep title="Wait for incoming requests">
      <p>
        Data Exchange will send you an email when someone requests access to
        your datasets.
      </p>
    </StartStep>
    {#if datasets.length > 0}
      <StartStep title="Upload more datasets">
        <p>
          You can always share new datasets with Data Exchange. They will
          automatically appear in "My Data Exchange" environment.
        </p>
      </StartStep>
    {/if}
  {/if}
</div>
