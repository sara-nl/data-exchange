<script lang="ts">
  import Spinner from './Spinner.svelte'
  import { getShares, Share, SharesResponse } from '../api/shares'
  import { mode } from '../stores'
  import { goto, stores } from '@sapper/app'

  let algorithms = null
  let datasets = null
  const updateInterval = 1000 // millis

  const watchShares: () => Promise<SharesResponse> = () => {
    return getShares().then(response => {
      if (response.own_algorithms === null || response.own_datasets === null) {
        return new Promise<SharesResponse>((res, rej) => {
          setTimeout(() => {
            console.log('Fetching shares again because prev was null')
            watchShares().then(res, rej)
          }, updateInterval)
        })
      }

      return Promise.resolve<SharesResponse>(response)
    })
  }

  watchShares().then(
    response => {
      algorithms = response.own_algorithms
      datasets = response.own_datasets
    },
    e => console.error('Error while loading user files', e)
  )
</script>

<div class="col my-3 p-4 px-5 bg-lightgrey rounded-xl">
  <h3>Where to start?</h3>
  {#if datasets === null || algorithms === null}
    <Spinner />
  {:else if (datasets.length > 0) & ($mode === 'data')}
    <div class="my-3">
      <p>
        You have shared {datasets.length} dataset(s) with the DataExchange
        <br />
        Click here to see any requests made for your data:
      </p>
      <button
        class="btn btn-primary rounded-xl font-weight-bold"
        on:click={() => goto(`/requests`)}>
        <div class="px-4">Go to your requests</div>
      </button>
    </div>
  {:else if (algorithms.length > 0) & ($mode === 'algorithm')}
    <div class="my-3">
      <p>
        You have shared {algorithms.length} algorithms with the DataExchange
        <br />
        Click here to make a request for the use of a dataset:
      </p>
      <button
        class="btn col-7 btn-primary rounded-xl font-weight-bold"
        on:click={() => goto(`/tasks/request`)}>

        <div class="px-4">Make request for or run with permission</div>
      </button>
    </div>
  {:else if datasets.length > 0 && $mode === 'data'}
    <div class="my-3">
      <p>
        You have shared datasets with the DataExchange
        <br />
        Click here to see any requests made for your data:
      </p>
      <button
        class="btn btn-primary rounded-xl font-weight-bold"
        on:click={() => goto(`/requests`)}>

        <div class="px-4">Go to your requests</div>
      </button>
    </div>
  {:else if algorithms.length > 0 && $mode === 'algorithm'}
    <div class="my-3">
      <p>
        You have shared {algorithms.length} algorithms with the DataExchange
        <br />
        Click here to make a request for the use of a dataset:
      </p>
      <button
        class="btn col-7 btn-primary rounded-xl font-weight-bold"
        on:click={() => goto(`/tasks/request`)}>

        <div class="px-4">Make request for or run with permission</div>
      </button>
    </div>
  {:else}
    <div class="my-3">
      <p>You haven't shared any files with the DataExchange</p>
    </div>
  {/if}

</div>
