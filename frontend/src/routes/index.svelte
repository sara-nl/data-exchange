<script lang="typescript">
  import Dashboard from '../components/Dashboard.svelte'

  import { token } from '../stores'
</script>

<svelte:head>
  <title>Data Exchange</title>
</svelte:head>

{#if Boolean($token)}
  <Dashboard />
{:else}
  <h2 class="display-4 text-primary font-weigth-bold ym-5">DataExchange</h2>

  <div class="container-fluid mx-auto">
    <div class="row my-5">
      <div class="col-8 ym-5 p-5 bg-lightgrey rounded-xl">
        <h3 class="ym-3">How does the DataExchange work?</h3>
        <p class="lead">
          The DataExchange offers a controlled and safe third-party environment
          where dataset and algorithm providers can safely cooperate and share
          data. The goal of DataExchange is to encourage sharing data while
          maintaining data sovereignty.
        </p>
        <p>
          On DataExchange, owners can share their datasets while staying in
          control. Algorithm owners can be given permissions to run on datasets,
          but will never be physically in control of it.
        </p>

        <h5 class="font-weight-bold">Secure container</h5>
        <p>
          The DataExchange provides a secure and networkless trusted third-party
          containers in which algorithm and dataset can safely interact. The
          dataset and algorithm are downloaded anonymously in the secure
          container and removed when the execution has been finished. Neither
          algorithm owner nor any other outside party will have physical hold of
          the dataset during this process.
        </p>
        <h5 class="font-weight-bold">Full data sovereignty</h5>

        <p>
          As owner of a dataset you can monitor all permissions, runs and review
          their output. All shared datasets are stored on your personal
          ResearchDrive and can be shared and unshared on your command at any
          time.
        </p>
      </div>
    </div>
  </div>
{/if}
